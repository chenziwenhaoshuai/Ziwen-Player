import SwiftUI
import UIKit

struct VideoCard: View {
    let item: VideoItem

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            PosterImage(url: item.poster)

            Text(item.title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(Theme.text)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text(item.displayMeta.isEmpty ? " " : item.displayMeta)
                .font(.system(size: 11))
                .foregroundColor(Theme.muted)
                .lineLimit(1)
        }
        .contentShape(Rectangle())
    }
}

/// 固定 2:3 的海报位：无论原图是横图还是竖图，格子尺寸都不变（超出部分裁切）
struct PosterImage: View {
    let url: String
    var cornerRadius: CGFloat = 12

    @State private var image: UIImage?

    var body: some View {
        ZStack {
            Theme.panel2
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                Image(systemName: "film")
                    .font(.system(size: 22))
                    .foregroundColor(Theme.muted2)
            }
        }
        .aspectRatio(2.0 / 3.0, contentMode: .fit)
        .frame(maxWidth: .infinity)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        .task(id: url) {
            image = await ImageCache.shared.image(for: url)
        }
    }
}

/// 带 Referer 的远程图片
struct RemoteImage: View {
    let url: String
    var contentMode: ContentMode = .fill
    var cornerRadius: CGFloat = 10

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
                    .foregroundColor(Theme.muted2)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        .task(id: url) {
            image = await ImageCache.shared.image(for: url)
        }
    }
}

struct ToastBanner: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 13, weight: .medium))
            .foregroundColor(Theme.text)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .glassEffect(.regular, in: .capsule)
    }
}

/// 分类胶囊（Liquid Glass）
struct CategoryChip: View {
    let title: String
    let active: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: active ? .bold : .medium))
                .foregroundColor(active ? .black : Theme.text)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
        .glassEffect(
            active ? .regular.tint(Theme.accent).interactive() : .regular.interactive(),
            in: .capsule
        )
    }
}

struct EmptyStateView: View {
    let icon: String
    let title: String
    var subtitle: String = ""

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 34))
                .foregroundColor(Theme.muted2)
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(Theme.text)
            if !subtitle.isEmpty {
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundColor(Theme.muted)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 80)
    }
}
