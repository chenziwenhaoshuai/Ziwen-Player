import SwiftUI

struct RecentScreen: View {
    @ObservedObject var model: AppModel

    var body: some View {
        ZStack {
            AppBackground()

            if model.recent.isEmpty {
                EmptyStateView(icon: "clock.arrow.circlepath", title: "暂无最近观看", subtitle: "看过的视频会出现在这里")
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(model.recent) { item in
                            NavigationLink(value: item) {
                                row(item)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 96)
                }
            }
        }
        .navigationTitle("最近观看")
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(for: VideoItem.self) { item in
            DetailScreen(model: model, video: item)
        }
        .toolbar {
            if !model.recent.isEmpty {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("清空") { model.clearHistory() }
                        .tint(Theme.danger)
                }
            }
        }
    }

    private func row(_ item: VideoItem) -> some View {
        HStack(spacing: 12) {
            RemoteImage(url: item.poster, cornerRadius: 10)
                .frame(width: 96, height: 54)

            VStack(alignment: .leading, spacing: 6) {
                Text(item.title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Theme.text)
                    .lineLimit(2)

                if !item.displayMeta.isEmpty {
                    Text(item.displayMeta)
                        .font(.system(size: 11))
                        .foregroundColor(Theme.muted)
                        .lineLimit(1)
                }

                if item.durationMs > 0 {
                    progressBar(item)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(systemName: "play.circle.fill")
                .font(.system(size: 22))
                .foregroundColor(Theme.accent.opacity(0.9))
        }
        .padding(10)
        .cardBackground(corner: 16)
    }

    private func progressBar(_ item: VideoItem) -> some View {
        let ratio = item.durationMs > 0 ? min(max(item.positionMs / item.durationMs, 0), 1) : 0
        return GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Theme.panel2)
                Capsule()
                    .fill(Theme.accent)
                    .frame(width: geo.size.width * ratio)
            }
        }
        .frame(height: 3)
    }
}
