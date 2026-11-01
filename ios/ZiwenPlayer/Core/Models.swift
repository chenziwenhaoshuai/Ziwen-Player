import Foundation

// MARK: - 视频条目

struct VideoItem: Identifiable, Hashable {
    var title: String = ""
    var url: String = ""
    var poster: String = ""
    var remarks: String = ""
    var provider: String = ""
    var remoteId: String = ""
    var playUrl: String = ""

    /// 仅最近观看使用
    var episodeTitle: String = ""
    var episodePath: String = ""
    var episodeFrom: String = ""
    var episodeSourceName: String = ""
    var episodeSource: Int = 0
    var episodeIndex: Int = 0

    var positionMs: Double = 0
    var durationMs: Double = 0
    var updatedAt: Double = 0

    var id: String { url }
    var isPeach: Bool { provider == "peach" }

    init(title: String, url: String, poster: String = "", remarks: String = "") {
        self.title = title
        self.url = url
        self.poster = poster
        self.remarks = remarks
    }

    init(title: String, url: String, poster: String, remarks: String,
         provider: String, remoteId: String, playUrl: String) {
        self.title = title
        self.url = url
        self.poster = poster
        self.remarks = remarks
        self.provider = provider
        self.remoteId = remoteId
        self.playUrl = playUrl
    }

    var displayMeta: String {
        guard episodeIndex > 0 else { return remarks }
        let label = episodeTitle.isEmpty ? "第\(episodeIndex)集" : episodeTitle
        return "\(label)  \(formatTime(positionMs))  \(remarks)"
    }

    func withProgress(episode: Episode, position: Double, duration: Double) -> VideoItem {
        var copy = self
        copy.episodeTitle = episode.title
        copy.episodePath = episode.path
        copy.episodeSource = episode.source
        copy.episodeIndex = episode.index
        copy.episodeFrom = episode.from
        copy.episodeSourceName = episode.sourceName
        copy.positionMs = normalizeRecentPosition(position, duration)
        copy.durationMs = max(0, duration)
        copy.updatedAt = Date().timeIntervalSince1970 * 1000
        return copy
    }
}

extension VideoItem: Codable {
    enum CodingKeys: String, CodingKey {
        case title, url, poster, remarks, provider, remoteId, playUrl
        case episodeTitle, episodePath, episodeFrom, episodeSourceName
        case episodeSource, episodeIndex, positionMs, durationMs, updatedAt
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        title = (try? c.decode(String.self, forKey: .title)) ?? ""
        url = (try? c.decode(String.self, forKey: .url)) ?? ""
        poster = (try? c.decode(String.self, forKey: .poster)) ?? ""
        remarks = (try? c.decode(String.self, forKey: .remarks)) ?? ""
        provider = (try? c.decode(String.self, forKey: .provider)) ?? ""
        remoteId = (try? c.decode(String.self, forKey: .remoteId)) ?? ""
        playUrl = (try? c.decode(String.self, forKey: .playUrl)) ?? ""
        episodeTitle = (try? c.decode(String.self, forKey: .episodeTitle)) ?? ""
        episodePath = (try? c.decode(String.self, forKey: .episodePath)) ?? ""
        episodeFrom = (try? c.decode(String.self, forKey: .episodeFrom)) ?? ""
        episodeSourceName = (try? c.decode(String.self, forKey: .episodeSourceName)) ?? ""
        episodeSource = (try? c.decode(Int.self, forKey: .episodeSource)) ?? 0
        episodeIndex = (try? c.decode(Int.self, forKey: .episodeIndex)) ?? 0
        positionMs = (try? c.decode(Double.self, forKey: .positionMs)) ?? 0
        durationMs = (try? c.decode(Double.self, forKey: .durationMs)) ?? 0
        updatedAt = (try? c.decode(Double.self, forKey: .updatedAt)) ?? 0
    }
}

// MARK: - 剧集

struct Episode: Identifiable, Hashable {
    let title: String
    let path: String
    let source: Int
    let index: Int
    let from: String
    let sourceName: String

    var id: String { path }

    init(_ title: String, _ path: String, _ source: Int, _ index: Int, _ from: String, _ sourceName: String) {
        self.title = title
        self.path = path
        self.source = source
        self.index = index
        self.from = from
        self.sourceName = sourceName
    }

    /// 原生播放优先级：越小越优
    var priority: Int {
        if Episode.isPreferredNativeSource(from) { return 0 }
        let f = from.lowercased()
        if f.contains("m3u8") || ["wolong", "360zy", "dplayer", "haiwaikan"].contains(f) { return 1 }
        return 5
    }

    var sourcePriority: Int {
        if !from.isEmpty { return priority }
        let name = sourceName
        if ["国际", "亚太", "备用", "海外"].contains(where: { name.contains($0) }) { return 0 }
        if name.contains("高清") { return 5 }
        return 2
    }

    var sourceLabel: String {
        if !sourceName.isEmpty { return sourceName }
        if !from.isEmpty { return priority <= 1 ? "m3u8" : from }
        return "线路\(source)"
    }

    static func isPreferredNativeSource(_ from: String) -> Bool {
        let f = from.lowercased()
        if f.contains("m3u8") { return true }
        return ["wolong", "360zy", "kuaikan", "leshi", "hw8", "haiwaikan", "dplayer"].contains(f)
    }
}

struct SourceGroup: Identifiable {
    let source: Int
    let title: String
    var episodes: [Episode]

    var id: Int { source }

    var priority: Int {
        if ["国际", "亚太", "备用", "海外", "m3u8"].contains(where: { title.contains($0) }) { return 0 }
        if title.contains("高清") { return 5 }
        return 2
    }
}

// MARK: - 详情

struct VideoDetail {
    var title: String
    var poster: String
    var description: String
    var meta: String
    var episodes: [Episode]

    init(title: String, poster: String, description: String, meta: String, episodes: [Episode]) {
        self.title = title
        self.poster = poster
        self.description = description.isEmpty ? "暂无简介" : description
        self.meta = meta
        self.episodes = episodes
    }

    var sourceGroups: [SourceGroup] {
        var order: [Int] = []
        var map: [Int: [Episode]] = [:]
        for episode in episodes {
            if map[episode.source] == nil { order.append(episode.source) }
            map[episode.source, default: []].append(episode)
        }
        var groups = order.map { id -> SourceGroup in
            let list = (map[id] ?? []).sorted { $0.index < $1.index }
            let name = list.first?.sourceName ?? ""
            return SourceGroup(source: id, title: name.isEmpty ? "线路\(id)" : name, episodes: list)
        }
        groups.sort {
            if $0.priority != $1.priority { return $0.priority < $1.priority }
            return $0.source < $1.source
        }
        return groups
    }
}

// MARK: - 播放目标

struct PlayTarget: Hashable {
    let title: String
    let webUrl: String
    let directUrl: String
    let from: String

    var isDirectVideo: Bool { !directUrl.isEmpty }
}

// MARK: - 分类

struct Category: Identifiable, Hashable {
    let name: String
    let path: String
    var id: String { path }
}

// MARK: - 工具

func normalizeRecentPosition(_ position: Double, _ duration: Double) -> Double {
    if position < 10_000 { return 0 }
    if duration > 0, duration - position <= 30_000 { return 0 }
    return duration > 0 ? min(position, duration) : position
}

func formatTime(_ ms: Double) -> String {
    let total = Int(max(0, ms) / 1000)
    let h = total / 3600
    let m = (total % 3600) / 60
    let s = total % 60
    return h > 0 ? String(format: "%d:%02d:%02d", h, m, s) : String(format: "%02d:%02d", m, s)
}

enum SiteError: LocalizedError {
    case badURL
    case http(Int)
    case message(String)
    case crypto

    var errorDescription: String? {
        switch self {
        case .badURL: return "地址无效"
        case .http(let code): return "请求失败（HTTP \(code)）"
        case .message(let text): return text
        case .crypto: return "数据解密失败"
        }
    }
}
