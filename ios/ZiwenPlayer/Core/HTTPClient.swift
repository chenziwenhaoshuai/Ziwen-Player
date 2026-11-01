import Foundation

/// 站点请求封装：复刻 Android 端的 UA / Referer / 超时，以及对 Peach 域名的证书宽容。
final class HTTPClient: NSObject, URLSessionDelegate {

    static let shared = HTTPClient()

    static let userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"
    static let playUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) ZiwenPlayer/1.0"

    /// 这些域名在 Android 上是 trust-all 兜底的
    private static let relaxedHosts = [
        "sm-api.wieuc.com",
        "hm-img.twmjjy.com",
        "hm-vip.twmjjy.com",
        "hm-img.aa66cc.live"
    ]

    private var session: URLSession!

    override init() {
        super.init()
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 10
        configuration.timeoutIntervalForResource = 30
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        configuration.httpShouldSetCookies = true
        session = URLSession(configuration: configuration, delegate: self, delegateQueue: nil)
    }

    func fetch(_ url: String, referer: String = "", timeout: TimeInterval = 8) async throws -> String {
        let data = try await fetchData(url, referer: referer, timeout: timeout)
        return String(decoding: data, as: UTF8.self)
    }

    func fetchData(_ url: String, referer: String = "", timeout: TimeInterval = 8, accept: String? = nil) async throws -> Data {
        guard let target = URL(string: url) else { throw SiteError.badURL }
        var request = URLRequest(url: target)
        request.timeoutInterval = timeout
        request.setValue(Self.userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue(
            accept ?? "text/html,application/xhtml+xml,application/xml,image/avif,image/webp,image/*,*/*;q=0.8",
            forHTTPHeaderField: "Accept"
        )
        if !referer.isEmpty {
            request.setValue(referer, forHTTPHeaderField: "Referer")
        }
        let (data, response) = try await session.data(for: request)
        if let http = response as? HTTPURLResponse, http.statusCode >= 400 {
            throw SiteError.http(http.statusCode)
        }
        return data
    }

    func urlSession(
        _ session: URLSession,
        didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void
    ) {
        let host = challenge.protectionSpace.host.lowercased()
        let relaxed = Self.relaxedHosts.contains { host == $0 || host.hasSuffix("." + $0) }
        if relaxed, challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust,
           let trust = challenge.protectionSpace.serverTrust {
            completionHandler(.useCredential, URLCredential(trust: trust))
            return
        }
        completionHandler(.performDefaultHandling, nil)
    }
}
