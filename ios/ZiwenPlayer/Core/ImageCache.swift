import Foundation
import UIKit

/// 图片加载 + 内存缓存，支持 Peach 的加密图片（.image + Fernet + data URL）。
@MainActor
final class ImageCache {

    static let shared = ImageCache()

    private let cache = NSCache<NSString, UIImage>()
    private var running: [String: Task<UIImage?, Never>] = [:]

    private init() {
        cache.countLimit = 300
    }

    func image(for url: String) async -> UIImage? {
        let trimmed = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        if let cached = cache.object(forKey: trimmed as NSString) { return cached }
        if let task = running[trimmed] { return await task.value }

        let task = Task<UIImage?, Never> { [weak self] in
            let image = await Self.load(url: trimmed)
            if let image {
                self?.cache.setObject(image, forKey: trimmed as NSString)
            }
            return image
        }
        running[trimmed] = task
        let image = await task.value
        running[trimmed] = nil
        return image
    }

    private static func load(url: String) async -> UIImage? {
        do {
            if url.contains(".image") {
                let text = try await HTTPClient.shared.fetch(url, referer: SiteClient.base + "/")
                return try decodeEncryptedImage(text)
            }
            let data = try await HTTPClient.shared.fetchData(
                url,
                referer: SiteClient.base + "/",
                accept: "image/jpeg,image/png,image/webp,image/*,*/*;q=0.8"
            )
            return UIImage(data: data)
        } catch {
            return nil
        }
    }

    private static func decodeEncryptedImage(_ raw: String) throws -> UIImage? {
        var payload = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if let separator = payload.range(of: "@@@") {
            let head = String(payload[payload.startIndex..<separator.lowerBound])
            let tail = String(payload[separator.upperBound...])
            payload = try Fernet.decrypt(token: head, key: SiteClient.peachKey) + tail
        } else {
            payload = try Fernet.decrypt(token: payload, key: SiteClient.peachKey)
        }
        if !payload.hasPrefix("data:") {
            payload = "data:image/jpeg;base64," + payload
        }
        guard let comma = payload.firstIndex(of: ",") else { return nil }
        let base64 = String(payload[payload.index(after: comma)...])
        guard let data = Data(base64Encoded: base64, options: .ignoreUnknownCharacters) else { return nil }
        return UIImage(data: data)
    }
}
