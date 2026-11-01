import Foundation
import SwiftUI

@MainActor
final class AppModel: ObservableObject {

    struct PlaybackRequest {
        let target: PlayTarget
        let episode: Episode
        let resume: Double
    }

    struct PlayerRequest: Identifiable {
        let id = UUID()
        let target: PlayTarget
        let episode: Episode?
        let resume: Double
    }

    // 列表
    @Published var title: String = "首页"
    @Published var path: String = "/"
    @Published var items: [VideoItem] = []
    @Published var loading: Bool = false
    @Published var loadingMore: Bool = false

    // 详情
    @Published var detail: VideoDetail?
    @Published var detailItem: VideoItem?
    @Published var detailLoading: Bool = false
    @Published var activeSource: Int = 0

    // 本地状态
    @Published var recent: [VideoItem] = []
    @Published var betaMode: Bool = Storage.betaMode
    @Published var preloadMinutes: Int = Storage.preloadMinutes

    // 搜索
    @Published var searchKeyword: String = ""
    @Published var searchResults: [VideoItem] = []
    @Published var searching: Bool = false

    // 播放器
    @Published var player: PlayerRequest?

    @Published var toast: String?

    private let client = SiteClient()
    private var page = 1
    private var pagingKind = ""
    private var pagedMode = false
    private var hasMore = false
    private var toastTask: Task<Void, Never>?

    static let categories: [Category] = [
        Category(name: "首页", path: "/"),
        Category(name: "电影", path: "/vod-show/1--time---------.html"),
        Category(name: "连续剧", path: "/vod-show/2--time---------.html"),
        Category(name: "综艺", path: "/vod-show/3--time---------.html"),
        Category(name: "动漫", path: "/vod-show/4--time---------.html")
    ]

    static let peachCategory = Category(name: "你懂的", path: SiteClient.peachPath)

    init() {
        recent = Storage.loadRecent(betaMode: betaMode)
    }

    var visibleCategories: [Category] {
        betaMode ? Self.categories + [Self.peachCategory] : Self.categories
    }

    var isPeachSelected: Bool { path.hasPrefix(SiteClient.peachPath) }

    var version: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }

    // MARK: - 提示

    func showToast(_ text: String) {
        toast = text
        toastTask?.cancel()
        toastTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 2_200_000_000)
            guard !Task.isCancelled else { return }
            self?.toast = nil
        }
    }

    // MARK: - 列表

    func select(_ category: Category) {
        title = category.name
        path = category.path
        page = 1
        pagingKind = Self.pagingKind(for: category.path)
        pagedMode = !pagingKind.isEmpty
        hasMore = pagedMode
        items = []
        load(reset: true)
    }

    private func load(reset: Bool) {
        if reset && path.hasPrefix(SiteClient.peachPath) && !betaMode {
            select(Self.categories[0])
            return
        }
        let requestedPath = reset ? path : Self.pagedPath(kind: pagingKind, page: page)
        if reset { loading = true }
        Task {
            do {
                let result = try await client.fetchCatalog(requestedPath)
                if reset {
                    items = result
                } else {
                    var seen = Set(items.map { $0.url })
                    for item in result where seen.insert(item.url).inserted {
                        items.append(item)
                    }
                }
                if result.isEmpty {
                    hasMore = false
                    if !reset { showToast("没有更多内容了") }
                }
                if reset && items.isEmpty { showToast("没有加载到内容") }
            } catch {
                if reset { items = [] }
                hasMore = false
                showToast(error.localizedDescription)
            }
            loading = false
            loadingMore = false
        }
    }

    func loadMoreIfNeeded(current: VideoItem) {
        guard pagedMode, hasMore, !loadingMore, !loading else { return }
        guard let index = items.firstIndex(where: { $0.url == current.url }) else { return }
        guard index >= items.count - 8 else { return }
        loadingMore = true
        page += 1
        load(reset: false)
    }

    func refreshPeachRandomly() {
        guard isPeachSelected else { return }
        let random = Int.random(in: 1...30)
        page = random
        pagedMode = true
        pagingKind = "peach"
        hasMore = true
        loading = true
        Task {
            let result = (try? await client.fetchCatalog(SiteClient.peachPath + "?page=\(random)")) ?? []
            items = result
            loading = false
            if result.isEmpty { showToast("没有内容") }
        }
    }

    static func pagingKind(for path: String) -> String {
        if path.hasPrefix(SiteClient.peachPath) { return "peach" }
        switch path {
        case "/vod-show/1--time---------.html": return "movie"
        case "/vod-show/2--time---------.html": return "2"
        case "/vod-show/3--time---------.html": return "3"
        case "/vod-show/4--time---------.html": return "4"
        default: return ""
        }
    }

    static func pagedPath(kind: String, page: Int) -> String {
        if kind == "peach" { return SiteClient.peachPath + "?page=\(page)" }
        let id: String
        switch kind {
        case "movie": id = "1"
        case "2": id = "2"
        case "3": id = "3"
        case "4": id = "4"
        default: return "/"
        }
        if page <= 1 { return "/vod-show/\(id)--time---------.html" }
        return "/vod-show/\(id)--time------\(page)---.html"
    }

    // MARK: - 搜索

    func performSearch(_ keyword: String) {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            showToast("请输入关键词")
            return
        }
        searchKeyword = trimmed
        searching = true
        Task {
            do {
                searchResults = try await client.fetchCatalog(SiteClient.searchPath(trimmed))
            } catch {
                searchResults = []
                showToast(error.localizedDescription)
            }
            searching = false
            if searchResults.isEmpty { showToast("没有找到相关内容") }
        }
    }

    // MARK: - 详情

    func loadDetail(_ item: VideoItem) async {
        if detailItem?.url == item.url, detail != nil { return }
        detailItem = item
        detail = nil
        detailLoading = true
        do {
            let loaded = try await client.fetchDetail(item)
            detail = loaded
            let groups = loaded.sourceGroups
            activeSource = groups.first(where: { $0.source == item.episodeSource })?.source
                ?? groups.first?.source
                ?? 0
        } catch {
            showToast(error.localizedDescription)
        }
        detailLoading = false
    }

    var activeEpisodes: [Episode] {
        guard let detail else { return [] }
        return detail.sourceGroups.first(where: { $0.source == activeSource })?.episodes ?? []
    }

    var activeGroups: [SourceGroup] {
        detail?.sourceGroups ?? []
    }

    // MARK: - 播放

    /// 解析某集的可用直链；全部候选都不是直链时返回 fallback（需要 WebView 解析）。
    func resolve(_ episode: Episode) async -> PlaybackRequest? {
        guard let item = detailItem else { return nil }
        let resume = resumePosition(for: episode, item: item)
        var fallback: PlayTarget?

        for candidate in playbackCandidates(for: episode) {
            guard let target = try? await client.resolvePlayTarget(candidate) else { continue }
            if target.isDirectVideo {
                return PlaybackRequest(target: target, episode: episode, resume: resume)
            }
            if fallback == nil { fallback = target }
        }

        guard let target = fallback else {
            showToast("没有可用的播放线路")
            return nil
        }
        return PlaybackRequest(target: target, episode: episode, resume: resume)
    }

    private func playbackCandidates(for episode: Episode) -> [Episode] {
        var list: [Episode] = []
        var seen = Set<String>()
        for candidate in activeEpisodes where candidate.index == episode.index {
            if seen.insert(candidate.path).inserted { list.append(candidate) }
        }
        if seen.insert(episode.path).inserted { list.append(episode) }
        list.sort { left, right in
            if left.sourcePriority != right.sourcePriority { return left.sourcePriority < right.sourcePriority }
            return left.source < right.source
        }
        return list
    }

    private func resumePosition(for episode: Episode, item: VideoItem) -> Double {
        guard episode.index == item.episodeIndex else { return 0 }
        let matches = episode.path == item.episodePath
            || episode.source == item.episodeSource
            || (!episode.sourceName.isEmpty && episode.sourceName == item.episodeSourceName)
            || (!episode.from.isEmpty && episode.from == item.episodeFrom)
        return matches ? item.positionMs : 0
    }

    func finishPlayback(episode: Episode?, position: Double, duration: Double) {
        guard let episode, var item = detailItem else { return }
        item = item.withProgress(episode: episode, position: position, duration: duration)
        detailItem = item
        recent = Storage.upsertRecent(item, into: recent)
        Storage.saveRecent(recent)
    }

    // MARK: - 设置

    func toggleBetaMode() {
        betaMode.toggle()
        Storage.betaMode = betaMode
        recent = Storage.loadRecent(betaMode: betaMode)
        showToast(betaMode ? "内测模式已开启" : "内测模式已关闭")
        if !betaMode && isPeachSelected {
            select(Self.categories[0])
        }
    }

    func setPreloadMinutes(_ value: Int) {
        preloadMinutes = value
        Storage.preloadMinutes = value
    }

    func clearHistory() {
        Storage.clearHistory()
        recent = []
        showToast("已清除历史浏览记录")
    }
}
