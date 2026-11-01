import SwiftUI
import AVKit

/// 页内播放器：AVPlayerViewController 自带播放/暂停、进度条、全屏、画中画、AirPlay。
struct InlineVideoView: UIViewControllerRepresentable {
    let player: AVPlayer
    var onFullscreenChange: (Bool) -> Void = { _ in }

    func makeCoordinator() -> Coordinator {
        Coordinator(onFullscreenChange: onFullscreenChange)
    }

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.player = player
        controller.showsPlaybackControls = true
        controller.allowsPictureInPicturePlayback = true
        controller.canStartPictureInPictureAutomaticallyFromInline = true
        controller.videoGravity = .resizeAspect
        controller.updatesNowPlayingInfoCenter = false
        controller.delegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ controller: AVPlayerViewController, context: Context) {
        if controller.player !== player {
            controller.player = player
        }
    }

    final class Coordinator: NSObject, AVPlayerViewControllerDelegate {
        private let onFullscreenChange: (Bool) -> Void

        init(onFullscreenChange: @escaping (Bool) -> Void) {
            self.onFullscreenChange = onFullscreenChange
        }

        func playerViewController(
            _ playerViewController: AVPlayerViewController,
            willBeginFullScreenPresentationWithAnimationCoordinator coordinator: UIViewControllerTransitionCoordinator
        ) {
            onFullscreenChange(true)
        }

        func playerViewController(
            _ playerViewController: AVPlayerViewController,
            willEndFullScreenPresentationWithAnimationCoordinator coordinator: UIViewControllerTransitionCoordinator
        ) {
            onFullscreenChange(false)
        }
    }
}

/// 顶部播放区域：16:9 黑底 + 18 圆角，右上角关闭按钮
struct InlinePlayerContainer: View {
    @ObservedObject var engine: PlaybackEngine
    var onFullscreenChange: (Bool) -> Void = { _ in }
    var onClose: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black
            InlineVideoView(player: engine.player, onFullscreenChange: onFullscreenChange)

            if engine.failed {
                VStack(spacing: 8) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 24))
                        .foregroundColor(Theme.accent)
                    Text(engine.status.isEmpty ? "播放失败" : engine.status)
                        .font(.system(size: 12))
                        .foregroundColor(Theme.muted)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }

            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.white)
                    .padding(8)
                    .background(.black.opacity(0.55), in: .circle)
            }
            .padding(10)
        }
        .aspectRatio(16.0 / 9.0, contentMode: .fit)
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Theme.line, lineWidth: 1)
        )
    }
}
