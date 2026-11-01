import Foundation

/// 本地状态：对应 Android 端的 SharedPreferences + recent_watches_v2。
enum Storage {

    private static let prefs = UserDefaults.standard
    private static let recentKey = "recent_watches_v2"
    private static let preloadKey = "preload_minutes"
    private static let betaKey = "beta_mode"

    static let preloadOptions = [1, 2, 3, 5, 8]
    static let recentLimit = 40

    static var preloadMinutes: Int {
        get {
            let value = prefs.integer(forKey: preloadKey)
            return preloadOptions.contains(value) ? value : 3
        }
        set {
            prefs.set(preloadOptions.contains(newValue) ? newValue : 3, forKey: preloadKey)
        }
    }

    static var betaMode: Bool {
        get { prefs.bool(forKey: betaKey) }
        set { prefs.set(newValue, forKey: betaKey) }
    }

    static func loadRecent(betaMode: Bool) -> [VideoItem] {
        guard let text = prefs.string(forKey: recentKey), let data = text.data(using: .utf8) else {
            return []
        }
        guard let decoded = try? JSONDecoder().decode([VideoItem].self, from: data) else {
            prefs.removeObject(forKey: recentKey)
            return []
        }
        var output: [VideoItem] = []
        for var item in decoded {
            if item.provider.isEmpty, item.url.hasPrefix("peach://") {
                item.provider = "peach"
            }
            if item.remoteId.isEmpty, item.url.hasPrefix(SiteClient.peachPath + "/detail/") {
                item.remoteId = String(item.url.dropFirst((SiteClient.peachPath + "/detail/").count))
            }
            if !betaMode && item.isPeach { continue }
            if item.title.isEmpty || item.url.isEmpty { continue }
            output.append(item)
            if output.count >= recentLimit { break }
        }
        return output
    }

    static func saveRecent(_ items: [VideoItem]) {
        let limited = Array(items.prefix(recentLimit))
        guard let data = try? JSONEncoder().encode(limited), let text = String(data: data, encoding: .utf8) else {
            return
        }
        prefs.set(text, forKey: recentKey)
    }

    static func clearHistory() {
        prefs.removeObject(forKey: recentKey)
    }

    /// 插入或更新一条最近观看，按 url 去重并置顶
    static func upsertRecent(_ item: VideoItem, into list: [VideoItem]) -> [VideoItem] {
        var output = list.filter { $0.url != item.url }
        output.insert(item, at: 0)
        return Array(output.prefix(recentLimit))
    }
}
