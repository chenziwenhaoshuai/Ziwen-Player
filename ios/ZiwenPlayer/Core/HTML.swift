import Foundation

/// 与 Java 端 SiteClient 中完全一致的正则 / HTML 处理逻辑。
enum HTML {

    // MARK: - 正则缓存

    private final class RegexStore {
        var map: [String: NSRegularExpression] = [:]
    }

    private static let store = RegexStore()
    private static let lock = NSLock()

    static func regex(_ pattern: String) -> NSRegularExpression? {
        lock.lock()
        defer { lock.unlock() }
        if let cached = store.map[pattern] { return cached }
        guard let created = try? NSRegularExpression(
            pattern: pattern,
            options: [.caseInsensitive, .dotMatchesLineSeparators]
        ) else { return nil }
        store.map[pattern] = created
        return created
    }

    static func matches(_ pattern: String, in text: String) -> [NSTextCheckingResult] {
        guard let re = regex(pattern) else { return [] }
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        return re.matches(in: text, options: [], range: range)
    }

    static func group(_ text: String, _ result: NSTextCheckingResult, _ index: Int) -> String {
        guard index < result.numberOfRanges else { return "" }
        let r = result.range(at: index)
        guard r.location != NSNotFound, let swiftRange = Range(r, in: text) else { return "" }
        return String(text[swiftRange])
    }

    // MARK: - 基础工具

    static func attr(_ tag: String, _ name: String) -> String? {
        let pattern = "\\b" + NSRegularExpression.escapedPattern(for: name) + "\\s*=\\s*[\"']([^\"']*)[\"']"
        guard let result = matches(pattern, in: tag).first else { return nil }
        return group(tag, result, 1)
    }

    static func stripTags(_ text: String) -> String {
        text.replacingOccurrences(of: "<[^>]+>", with: " ", options: .regularExpression)
    }

    static func decode(_ text: String) -> String {
        var output = text
        let named = [
            "&nbsp;": " ", "&amp;": "&", "&lt;": "<", "&gt;": ">",
            "&quot;": "\"", "&#39;": "'", "&apos;": "'",
            "&ldquo;": "“", "&rdquo;": "”", "&hellip;": "…",
            "&middot;": "·", "&mdash;": "—", "&ndash;": "–"
        ]
        for (key, value) in named {
            output = output.replacingOccurrences(of: key, with: value)
        }
        output = decodeNumericEntities(output)
        return output
    }

    private static func decodeNumericEntities(_ text: String) -> String {
        guard text.contains("&#") else { return text }
        var output = ""
        var rest = Substring(text)
        while let start = rest.range(of: "&#") {
            output += rest[rest.startIndex..<start.lowerBound]
            let tail = rest[start.lowerBound...]
            guard let semi = tail.firstIndex(of: ";") else {
                output += tail
                return output
            }
            let body = tail[tail.index(tail.startIndex, offsetBy: 2)..<semi]
            var scalar: UInt32?
            if body.hasPrefix("x") || body.hasPrefix("X") {
                scalar = UInt32(body.dropFirst(), radix: 16)
            } else {
                scalar = UInt32(body)
            }
            if let value = scalar, let unicode = Unicode.Scalar(value) {
                output.append(Character(unicode))
            } else {
                output += String(tail[tail.startIndex...semi])
            }
            rest = tail[tail.index(after: semi)...]
        }
        output += String(rest)
        return output
    }

    static func cleanText(_ raw: String) -> String {
        var text = stripTags(raw)
        text = decode(text)
        text = text.replacingOccurrences(of: "\u{00A0}", with: " ")
        text = text.replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
        return text.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    static func between(_ text: String, _ start: String, _ end: String) -> String {
        guard let left = text.range(of: start) else { return "" }
        let rest = text[left.upperBound...]
        guard let right = rest.range(of: end) else { return "" }
        return String(rest[rest.startIndex..<right.lowerBound])
    }

    static func firstText(_ chunk: String, _ cls: String) -> String {
        let pattern = "<[^>]+class=[\"'][^\"']*" + NSRegularExpression.escapedPattern(for: cls) + "[^\"']*[\"'][^>]*>(.*?)</[^>]+>"
        guard let result = matches(pattern, in: chunk).first else { return "" }
        return cleanText(group(chunk, result, 1))
    }

    static func backgroundUrl(_ tag: String) -> String {
        let pattern = "background-image\\s*:\\s*url\\((['\"]?)(.*?)\\1\\)"
        guard let result = matches(pattern, in: tag).first else { return "" }
        return group(tag, result, 2)
    }

    static func firstMeta(_ html: String, _ key: String) -> String {
        let escaped = NSRegularExpression.escapedPattern(for: key)
        let patterns = [
            "<meta[^>]+(?:name|property)=[\"']" + escaped + "[\"'][^>]+content=[\"']([^\"']*)[\"']",
            "<meta[^>]+content=[\"']([^\"']*)[\"'][^>]+(?:name|property)=[\"']" + escaped + "[\"']"
        ]
        for pattern in patterns {
            if let result = matches(pattern, in: html).first {
                let value = group(html, result, 1)
                if !value.isEmpty { return decode(value) }
            }
        }
        return ""
    }

    /// 详情页 meta：状态 / 年份 / 类型 / 主演
    static func collectMeta(_ html: String) -> String {
        let order = ["状态：", "年份：", "类型：", "主演："]
        var parts: [String] = []
        for label in order {
            let value = field(html, label)
            if !value.isEmpty { parts.append(value) }
        }
        return parts.isEmpty ? "来自爱壹帆" : parts.joined(separator: " / ")
    }

    static func field(_ html: String, _ label: String) -> String {
        guard let range = html.range(of: label) else { return "" }
        let tail = html[range.upperBound...]
        let limited = tail.prefix(220)
        var text = stripTags(String(limited))
        text = decode(text)
        text = text.replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
        text = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if let space = text.firstIndex(of: " ") {
            text = String(text[text.startIndex..<space])
        }
        return text.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    static func firstIframe(_ html: String) -> String {
        let patterns = [
            "<iframe\\b[^>]*id=[\"']player_if[\"'][^>]*src=[\"']([^\"']+)[\"']",
            "<iframe\\b[^>]*src=[\"']([^\"']*/vod/player/[^\"']+)[\"']"
        ]
        for pattern in patterns {
            if let result = matches(pattern, in: html).first {
                let value = group(html, result, 1)
                if !value.isEmpty { return value }
            }
        }
        return ""
    }

    static func playerValue(_ html: String, _ key: String) -> String {
        let escaped = NSRegularExpression.escapedPattern(for: key)
        let pattern = "player_aaaa\\s*=\\s*\\{.*?[\"']" + escaped + "[\"']\\s*:\\s*[\"'](.*?)[\"']"
        guard let result = matches(pattern, in: html).first else { return "" }
        var value = group(html, result, 1)
        value = value.replacingOccurrences(of: "\\/", with: "/")
        if value.hasPrefix("//") { value = "https:" + value }
        return value
    }

    static func absolutize(_ path: String, base: String) -> String {
        let trimmed = path.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return "" }
        if trimmed.hasPrefix("http://") || trimmed.hasPrefix("https://") || trimmed.hasPrefix("data:") {
            return trimmed
        }
        if trimmed.hasPrefix("//") { return "https:" + trimmed }
        var host = base
        if let url = URL(string: base), let scheme = url.scheme, let hostName = url.host {
            host = scheme + "://" + hostName
        }
        return host + (trimmed.hasPrefix("/") ? trimmed : "/" + trimmed)
    }
}
