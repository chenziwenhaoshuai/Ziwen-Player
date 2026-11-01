import Foundation
import UIKit

/// 图片加载 + 内存缓存。
///
/// 站点图床（oss.bubitech.cn）的证书经常过期，系统会直接拒收，
/// 所以图片走一个独立的、允许自签/过期证书的 Session；
/// 同时支持 Peach 的加密图片（.image + Fernet + data URL）。
@MainActor
final class ImageCache {

    static let shared = ImageCache()

    private let cache = NSCache<NSString, UIImage>()
    private var running: [String: Task<UIImage?, Never>] = [:]
    private let session: URLSession

    private init() {
        cache.countLimit = 300
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 20
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        session = URLSession(configuration: config, delegate: ImageTrustDelegate(), delegateQueue: nil)
    }

    func image(for url: String) async -> UIImage? {
        let trimmed = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        if let cached = cache.object(forKey: trimmed as NSString) { return cached }
        if let task = running[trimmed] { return await task.value }

        let task = Task<UIImage?, Never> { [weak self] in
            let image = await self?.load(url: trimmed)
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

    private func load(url: String) async -> UIImage? {
        do {
            if url.contains(".image") {
                let text = try await fetchText(url)
                return try Self.decodeEncryptedImage(text)
            }
            let data = try await fetchData(url)
            guard let image = UIImage(data: data) else {
                NSLog("[ZWImage] 解码失败 %@ (%d bytes)", url, data.count)
                return nil
            }
            return image
        } catch {
            NSLog("[ZWImage] 加载失败 %@ -> %@", url, error.localizedDescription)
            return nil
        }
    }

    private func fetchData(_ urlString: String) async throws -> Data {
        guard let url = URL(string: urlString) else { throw SiteError.badURL }
        var request = URLRequest(url: url)
        request.setValue(HTTPClient.userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("image/avif,image/webp,image/*,*/*;q=0.8", forHTTPHeaderField: "Accept")
        // 图床不校验 Referer，带上站点 Referer 反而容易被拒
        let (data, response) = try await session.data(for: request)
        if let http = response as? HTTPURLResponse, http.statusCode >= 400 {
            throw SiteError.http(http.statusCode)
        }
        return data
    }

    private func fetchText(_ urlString: String) async throws -> String {
        let data = try await fetchData(urlString)
        return String(decoding: data, as: UTF8.self)
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

/// 图片专用：接受过期 / 自签证书（图床证书经常出问题）
private final class ImageTrustDelegate: NSObject, URLSessionDelegate {
    func urlSession(
        _ session: URLSession,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        if challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust,
           let trust = challenge.protectionSpace.serverTrust {
            completionHandler(.useCredential, URLCredential(trust: trust))
            return
        }
        completionHandler(.performDefaultHandling, nil)
    }
}
