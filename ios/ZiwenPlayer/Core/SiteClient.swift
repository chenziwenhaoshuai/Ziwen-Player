import Foundation

/// 站点数据接口：等价于 Android 端的 SiteClient。
struct SiteClient {

    static let base = "https://www.yfvod.com"

    static let peachPath = "peach://catalog"
    static let peachAPI = "https://sm-api.wieuc.com"
    static let peachSiteId = "2"
    static let peachChannelId = "522"
    static let peachChannelName = "gj-89"
    static let peachImageHost = "https://hm-img.twmjjy.com"
    static let peachPlayHosts = [
        "https://hm-img.twmjjy.com",
        "https://hm-vip.twmjjy.com",
        "https://hm-img.aa66cc.live"
    ]
    static let peachKey = "NyGRG56A8i5J2JMqh7da83r2MMfgbM7Ppw1aCF8YnAY="
    static let peachPerPage = 24

    private let http = HTTPClient.shared

    // MARK: - 列表

    func fetchCatalog(_ path: String) async throws -> [VideoItem] {
        if path.hasPrefix(Self.peachPath) {
            return try await fetchPeachCatalog(page: Self.peachPage(path))
        }
        let pageURL = HTML.absolutize(path, base: Self.base + "/")
        let html = try await http.fetch(pageURL, referer: Self.base + "/")

        var output: [VideoItem] = []
        var seen = Set<String>()
        let anchorPattern = "<a\\b[^>]*>"

        for result in HTML.matches(anchorPattern, in: html) {
            let tag = HTML.group(html, result, 0)
            guard let href = HTML.attr(tag, "href"), href.contains("/vodhtml/") else { continue }

            var title = HTML.attr(tag, "title") ?? ""
            if title.isEmpty {
                let cls = HTML.attr(tag, "class") ?? ""
                guard cls.contains("hl-item-thumb") || cls.contains("hl-br-thumb") else { continue }
            }
            if title.isEmpty { title = HTML.cleanText(tag) }
            if title.isEmpty { continue }

            var poster = HTML.attr(tag, "data-original") ?? ""
            if poster.isEmpty { poster = HTML.backgroundUrl(tag) }

            let chunk = contextChunk(html: html, anchor: result)
            let remarks = HTML.firstText(chunk, "remarks")
            let score = HTML.firstText(chunk, "score")
            let meta = score.isEmpty ? remarks : score + "  " + remarks

            let url = HTML.absolutize(href, base: Self.base + "/")
            guard seen.insert(url).inserted else { continue }
            output.append(VideoItem(
                title: title,
                url: url,
                poster: HTML.absolutize(poster, base: Self.base),
                remarks: meta
            ))
        }
        return output
    }

    private func contextChunk(html: String, anchor: NSTextCheckingResult) -> String {
        let ns = html as NSString
        guard anchor.range.location != NSNotFound else { return "" }
        let before = NSRange(location: 0, length: anchor.range.location)
        let liStart = ns.range(of: "<li", options: .backwards, range: before)
        let tailStart = anchor.range.location + anchor.range.length
        let tail = NSRange(location: tailStart, length: ns.length - tailStart)
        let liEnd = ns.range(of: "</li>", options: [], range: tail)
        guard liStart.location != NSNotFound, liEnd.location != NSNotFound else {
            return ns.substring(with: anchor.range)
        }
        let start = liStart.location
        let end = min(liEnd.location + liEnd.length, ns.length)
        return ns.substring(with: NSRange(location: start, length: end - start))
    }

    /// 站内搜索：复用列表解析，路径规则与 Android 端一致（13 个连字符）
    static func searchPath(_ keyword: String) -> String {
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-._~"))
        let encoded = keyword.addingPercentEncoding(withAllowedCharacters: allowed) ?? keyword
        return "/vod-search/\(encoded)-------------.html"
    }

    // MARK: - 详情

    func fetchDetail(_ item: VideoItem) async throws -> VideoDetail {
        if item.isPeach {
            return try await fetchPeachDetail(item)
        }
        let html = try await http.fetch(item.url, referer: Self.base + "/")

        var title = item.title
        let pageTitle = HTML.between(html, "<title>", "</title>")
        if !pageTitle.isEmpty, let left = pageTitle.firstIndex(of: "《") {
            let rest = pageTitle[pageTitle.index(after: left)...]
            if let right = rest.firstIndex(of: "》") {
                title = String(rest[rest.startIndex..<right])
            }
        }

        var poster = HTML.firstMeta(html, "og:image")
        if poster.isEmpty { poster = item.poster }
        let description = HTML.firstMeta(html, "description")
        let meta = HTML.collectMeta(html)
        let sourceNames = parseSourceNames(html)

        var episodes: [Episode] = []
        var seenPaths = Set<String>()
        let playPattern = "<a\\b([^>]*)href=[\"']([^\"']*vod-play/([0-9]+)-(\\d+)-(\\d+)\\.html)[\"']([^>]*)>(.*?)</a>"

        for result in HTML.matches(playPattern, in: html) {
            let href = HTML.group(html, result, 2)
            let sid = Int(HTML.group(html, result, 4)) ?? 0
            let nid = Int(HTML.group(html, result, 5)) ?? 0
            var text = HTML.cleanText(HTML.group(html, result, 7))
            if text.isEmpty { text = "第\(nid)集" }
            if sid > 1 && !text.contains("线路") { text = "线路\(sid) " + text }
            let path = HTML.absolutize(href, base: Self.base)
            guard seenPaths.insert(path).inserted else { continue }
            episodes.append(Episode(text, path, sid, nid, "", sourceNames[sid] ?? ""))
        }

        episodes.sort { left, right in
            if left.index != right.index { return left.index < right.index }
            if left.sourcePriority != right.sourcePriority { return left.sourcePriority < right.sourcePriority }
            return left.source < right.source
        }

        return VideoDetail(title: title, poster: poster, description: description, meta: meta, episodes: episodes)
    }

    private func parseSourceNames(_ html: String) -> [Int: String] {
        var numbers: [Int] = []
        for result in HTML.matches("/vod-play/\\d+-(\\d+)-1\\.html", in: html) {
            guard let value = Int(HTML.group(html, result, 1)) else { continue }
            if !numbers.contains(value) { numbers.append(value) }
        }
        var names: [String] = []
        let namePattern = "<a\\b[^>]*class=[\"'][^\"']*(?:hl-from-btn|hl-tabs-btn)[^\"']*[\"'][^>]*>(.*?)</a>"
        for result in HTML.matches(namePattern, in: html) {
            let name = HTML.cleanText(HTML.group(html, result, 1))
            if !name.isEmpty, !names.contains(name) { names.append(name) }
        }
        var map: [Int: String] = [:]
        for (index, number) in numbers.prefix(names.count).enumerated() {
            map[number] = names[index]
        }
        return map
    }

    // MARK: - 播放地址解析

    func resolvePlayTarget(_ episode: Episode) async throws -> PlayTarget {
        if episode.from == "peach" {
            return PlayTarget(title: episode.title, webUrl: episode.path, directUrl: episode.path, from: "peach")
        }
        let playHtml = try await http.fetch(episode.path, referer: Self.base + "/")
        let iframe = HTML.firstIframe(playHtml)

        var playerHtml = playHtml
        if !iframe.isEmpty {
            let iframeURL = HTML.absolutize(iframe, base: Self.base)
            playerHtml = try await http.fetch(iframeURL, referer: episode.path)
        }

        let url = HTML.playerValue(playerHtml, "url")
        let from = HTML.playerValue(playerHtml, "from")
        let lowered = url.lowercased()
        let isDirect = [".m3u8", ".mp4", ".flv", ".webm"].contains { lowered.contains($0) }

        return PlayTarget(
            title: episode.title,
            webUrl: HTML.absolutize(episode.path, base: Self.base),
            directUrl: isDirect ? url : "",
            from: from
        )
    }

    // MARK: - Peach

    static func peachPage(_ path: String) -> Int {
        guard let range = path.range(of: "page=") else { return 1 }
        let digits = path[range.upperBound...].prefix { $0.isNumber }
        return max(1, Int(digits) ?? 1)
    }

    private func fetchPeachCatalog(page: Int) async throws -> [VideoItem] {
        let url = Self.peachAPI
            + "/api/vod/video?site_id=" + Self.peachSiteId
            + "&channel_id=" + Self.peachChannelId
            + "&channel_name=" + Self.peachChannelName
            + "&page=" + String(page)
            + "&per_page=" + String(Self.peachPerPage)

        let data = try await fetchPeachData(url)
        let items = data["items"] as? [[String: Any]] ?? []
        var output: [VideoItem] = []

        for item in items {
            let id = text(item["id"])
            let title = text(item["name"])
            guard !id.isEmpty, !title.isEmpty else { continue }
            let poster = Self.peachImageUrl(text(item["pic"]))
            let playUrl = text(item["play_url"])
            let duration = text(item["duration"])
            let pubdate = text(item["pubdate"])
            let meta = duration.isEmpty ? pubdate : duration + (pubdate.isEmpty ? "" : "  " + pubdate)
            output.append(VideoItem(
                title: title,
                url: Self.peachPath + "/detail/" + id,
                poster: poster,
                remarks: meta,
                provider: "peach",
                remoteId: id,
                playUrl: playUrl
            ))
        }

        output.sort { Self.peachPubdate($0) > Self.peachPubdate($1) }
        return output
    }

    private func fetchPeachDetail(_ item: VideoItem) async throws -> VideoDetail {
        var data: [String: Any] = [:]
        if !item.remoteId.isEmpty {
            let url = Self.peachAPI + "/api/vod/video/" + item.remoteId
                + "?site_id=" + Self.peachSiteId
                + "&channel_id=" + Self.peachChannelId
                + "&channel_name=" + Self.peachChannelName
            data = try await fetchPeachData(url)
        }

        let title = valueOr(text(data["name"]), item.title)
        let poster = Self.peachImageUrl(valueOr(text(data["pic"]), item.poster))
        let playPath = valueOr(text(data["play_url"]), item.playUrl)
        let description = valueOr(text(data["description"]), title)
        let duration = text(data["duration"])
        let pubdate = text(data["pubdate"])
        let meta = duration.isEmpty ? pubdate : duration + (pubdate.isEmpty ? "" : "  " + pubdate)

        var episodes: [Episode] = []
        if !playPath.isEmpty {
            for (index, host) in Self.peachPlayHosts.enumerated() {
                let url = Self.absolutizeHost(host, playPath)
                guard !url.isEmpty else { continue }
                episodes.append(Episode("播放", url, index + 1, 1, "peach", "线路\(index + 1)"))
            }
        }

        return VideoDetail(title: title, poster: poster, description: description, meta: meta, episodes: episodes)
    }

    private func fetchPeachData(_ url: String) async throws -> [String: Any] {
        let raw = try await http.fetch(url, referer: "")
        var payload = (try? JSONSerialization.jsonObject(with: Data(raw.utf8))) as? [String: Any] ?? [:]

        if let encrypted = payload["x-data"] as? String, !encrypted.isEmpty {
            let decrypted = try Fernet.decrypt(token: encrypted, key: Self.peachKey)
            payload = (try? JSONSerialization.jsonObject(with: Data(decrypted.utf8))) as? [String: Any] ?? [:]
        }

        let code = (payload["code"] as? NSNumber)?.intValue ?? 0
        if code != 0 {
            throw SiteError.message(text(payload["message"], fallback: "API error"))
        }
        return payload["data"] as? [String: Any] ?? [:]
    }

    static func peachImageUrl(_ path: String) -> String {
        absolutizeHost(peachImageHost, path)
    }

    static func absolutizeHost(_ host: String, _ path: String) -> String {
        guard !path.isEmpty else { return "" }
        if path.hasPrefix("http://") || path.hasPrefix("https://") || path.hasPrefix("data:") {
            return path
        }
        return host + (path.hasPrefix("/") ? path : "/" + path)
    }

    private static func peachPubdate(_ item: VideoItem) -> String {
        guard !item.remarks.isEmpty else { return "" }
        if let result = HTML.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{8}", in: item.remarks).first {
            return HTML.group(item.remarks, result, 0).replacingOccurrences(of: "/", with: "-")
        }
        return item.remarks
    }

    // MARK: - 小工具

    private func text(_ value: Any?) -> String {
        if let string = value as? String { return string }
        if let number = value as? NSNumber { return number.stringValue }
        return ""
    }

    private func text(_ value: Any?, fallback: String) -> String {
        let raw = text(value)
        return raw.isEmpty ? fallback : raw
    }

    private func valueOr(_ value: String, _ fallback: String) -> String {
        value.isEmpty ? fallback : value
    }
}
