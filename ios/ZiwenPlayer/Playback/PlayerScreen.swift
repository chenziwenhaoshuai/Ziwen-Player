import SwiftUI
import AVKit
import WebKit

/// 播放页：直链直接播；非直链先用隐藏 WebView 捕获 m3u8。
struct PlayerScreen: View {

    let request: AppModel.PlayerRequest
    let onProgress: (_ position: Double, _ duration: Double) -> Void
    let onClose: (_ position: Double, _ duration: Double) -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var player: AVPlayer?
    @State private var catcher: M3U8Catcher?
    @State private var statusText = "正在解析播放地址…"
    @State private var failed = false
    @State private var positionMs: Double = 0
    @State private var durationMs: Double = 0
    @State private var lastSavedAt: Date = .distantPast

    private let ticker = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.black.ignoresSafeArea()

            if let player {
                PlayerViewController(player: player)
                    .ignoresSafeArea()
            } else {
                VStack(spacing: 14) {
                    if failed {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 34))
                            .foregroundColor(Theme.accent)
                        Text(statusText)
                            .foregroundColor(Theme.muted)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 40)
                        HStack(spacing: 12) {
                            Button("重新加载") { start() }
                                .buttonStyle(PillButtonStyle(filled: true))
                            Button("返回") { close() }
                                .buttonStyle(PillButtonStyle(filled: false))
                        }
                    } else {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: Theme.accent))
                        Text(statusText)
                            .foregroundColor(Theme.muted)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }

            Button(action: close) {
                Image(systemName: "xmark")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.white)
                    .padding(10)
                    .background(Color.black.opacity(0.55))
                    .clipShape(Circle())
            }
            .padding(18)

            if let catcher {
                CatcherWebView(catcher: catcher)
                    .frame(width: 1, height: 1)
                    .opacity(0.01)
                    .allowsHitTesting(false)
            }
        }
        .statusBarHidden(true)
        .onAppear(perform: start)
        .onDisappear(perform: cleanup)
        .onReceive(ticker) { _ in tick() }
    }

    // MARK: - 流程

    private func start() {
        failed = false
        if request.target.isDirectVideo {
            statusText = "正在加载视频…"
            makePlayer(urlString: request.target.directUrl)
            return
        }

        let created = M3U8Catcher()
        created.onCapture = { url in
            statusText = "正在加载视频…"
            makePlayer(urlString: url)
        }
        created.onTimeout = {
            failed = true
            statusText = "未捕获到 m3u8，请换「国际」「亚太」等 m3u8 线路"
        }
        catcher = created
        statusText = "正在捕获视频流…"
        _ = created.webViewIfNeeded()
        created.start(request.target.webUrl)
    }

    private func makePlayer(urlString: String) {
        guard let url = URL(string: urlString), !urlString.isEmpty else {
            failed = true
            statusText = "播放地址无效"
            return
        }
        let item = AVPlayerItem(url: url)
        let created = AVPlayer(playerItem: item)
        created.automaticallyWaitsToMinimizeStalling = true
        created.allowsExternalPlayback = true
        player = created

        if request.resume > 0 {
            created.seek(to: CMTime(seconds: request.resume / 1000.0, preferredTimescale: 600))
        }
        created.play()
    }

    private func tick() {
        guard let player else { return }
        let seconds = player.currentTime().seconds
        if seconds.isFinite, seconds >= 0 {
            positionMs = seconds * 1000
        }
        if let total = player.currentItem?.duration.seconds, total.isFinite, total > 0 {
            durationMs = total * 1000
        }
        if Date().timeIntervalSince(lastSavedAt) > 15 {
            lastSavedAt = Date()
            onProgress(positionMs, durationMs)
        }
    }

    private func close() {
        let position = positionMs
        let duration = durationMs
        cleanup()
        onClose(position, duration)
        dismiss()
    }

    private func cleanup() {
        catcher?.stop()
        catcher = nil
        player?.pause()
        player = nil
    }
}

// MARK: - 系统播放器

private struct PlayerViewController: UIViewControllerRepresentable {
    let player: AVPlayer

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.player = player
        controller.showsPlaybackControls = true
        controller.allowsPictureInPicturePlayback = true
        controller.canStartPictureInPictureAutomaticallyFromInline = true
        controller.videoGravity = .resizeAspect
        return controller
    }

    func updateUIViewController(_ controller: AVPlayerViewController, context: Context) {
        if controller.player !== player {
            controller.player = player
        }
    }
}

private struct CatcherWebView: UIViewRepresentable {
    let catcher: M3U8Catcher

    func makeUIView(context: Context) -> WKWebView {
        catcher.webViewIfNeeded()
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}
}
