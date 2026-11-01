import Foundation
import AVFoundation
import OSLog

/// 页内播放引擎：详情页顶部的播放器，支持原生全屏（AVPlayerViewController 的展开按钮）。
@MainActor
final class PlaybackEngine: ObservableObject {

    let player = AVPlayer()

    @Published var episode: Episode?
    @Published var failed = false
    @Published var status = ""
    @Published var positionMs: Double = 0
    @Published var durationMs: Double = 0

    /// 每 15 秒回传一次进度，用于续播
    var onProgress: ((Double, Double) -> Void)?

    private var ticker: Task<Void, Never>?
    private var lastSaved = Date.distantPast
    private var wasPlayingBeforeFullscreen = false
    private let log = Logger(subsystem: "local.zw.player", category: "player")

    /// AVKit 在进出全屏时可能自动暂停，按"进全屏前是否在播"决定是否恢复
    func handleFullscreen(_ active: Bool) {
        if active {
            wasPlayingBeforeFullscreen = player.timeControlStatus == .playing
            log.notice("进入全屏，之前是否在播: \(self.wasPlayingBeforeFullscreen)")
        } else if wasPlayingBeforeFullscreen {
            player.play()
            log.notice("退出全屏，恢复播放")
        }
    }

    var isActive: Bool { episode != nil }

    func load(target: PlayTarget, episode: Episode, resume: Double) {
        stop()
        self.episode = episode
        failed = false
        lastSaved = .distantPast

        guard !target.directUrl.isEmpty, let url = URL(string: target.directUrl) else {
            failed = true
            status = "无法解析播放地址"
            return
        }

        let item = AVPlayerItem(asset: Self.makeAsset(url: url))
        player.replaceCurrentItem(with: item)
        player.automaticallyWaitsToMinimizeStalling = true
        if resume > 0 {
            player.seek(to: CMTime(seconds: resume / 1000, preferredTimescale: 600))
        }
        player.play()
        startTicker()
    }

    func stop() {
        ticker?.cancel()
        ticker = nil
        player.pause()
        player.replaceCurrentItem(with: nil)
        episode = nil
        positionMs = 0
        durationMs = 0
        failed = false
        status = ""
    }

    /// 离开页面时调用：先回传进度再停止
    func finish() {
        tick()
        onProgress?(positionMs, durationMs)
        stop()
    }

    private func startTicker() {
        ticker = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard !Task.isCancelled else { return }
                self?.tick()
            }
        }
    }

    private func tick() {
        guard let item = player.currentItem else { return }

        if item.status == .failed {
            failed = true
            status = item.error?.localizedDescription ?? "播放失败"
            return
        }

        let seconds = player.currentTime().seconds
        if seconds.isFinite, seconds >= 0 {
            positionMs = seconds * 1000
        }
        let total = item.duration.seconds
        if total.isFinite, total > 0 {
            durationMs = total * 1000
        }

        if Date().timeIntervalSince(lastSaved) > 15 {
            lastSaved = Date()
            onProgress?(positionMs, durationMs)
        }
    }

    /// 站点分片同样需要 Referer / UA，否则会被 CDN 拒绝
    static func makeAsset(url: URL) -> AVURLAsset {
        let headers: [String: String] = [
            "Referer": SiteClient.base + "/",
            "User-Agent": HTTPClient.userAgent,
            "Accept": "*/*"
        ]
        return AVURLAsset(url: url, options: ["AVURLAssetHTTPHeaderFieldsKey": headers])
    }
}
