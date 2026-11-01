import SwiftUI
import UIKit

/// 带 Referer 的远程图片（系统 AsyncImage 无法设置请求头）
struct RemoteImage: View {
    let url: String
    var contentMode: ContentMode = .fill
    var cornerRadius: CGFloat = 8

    @State private var image: UIImage?

    var body: some View {
        ZStack {
            Theme.panel2
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: contentMode)
            } else {
                Image(systemName: "film")
                    .font(.system(size: 22))
                    .foregroundColor(Theme.muted.opacity(0.45))
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        .task(id: url) {
            image = await ImageCache.shared.image(for: url)
        }
    }
}

struct PillButtonStyle: ButtonStyle {
    var filled: Bool = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 15, weight: .semibold))
            .foregroundColor(filled ? .black : Theme.text)
            .padding(.horizontal, 18)
            .padding(.vertical, 10)
            .background(filled ? Theme.accent : Theme.panel)
            .clipShape(Capsule())
            .opacity(configuration.isPressed ? 0.7 : 1)
    }
}

struct ToastView: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 15))
            .foregroundColor(.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .background(Color.black.opacity(0.85))
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .padding(.bottom, 30)
            .transition(.opacity)
    }
}

struct SectionTitle: View {
    let text: String
    let size: CGFloat

    var body: some View {
        Text(text)
            .font(.system(size: size, weight: .bold))
            .foregroundColor(Theme.text)
    }
}
