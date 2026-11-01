import SwiftUI

struct RootView: View {
    @StateObject private var model = AppModel()

    var body: some View {
        GeometryReader { geo in
            let metrics = Metrics(screenHeight: geo.size.height)

            ZStack(alignment: .bottom) {
                Theme.bg.ignoresSafeArea()

                HStack(spacing: 0) {
                    if model.route != .detail {
                        SidebarView(model: model, metrics: metrics)
                    }
                    content(metrics: metrics)
                }

                if let toast = model.toast {
                    ToastView(text: toast)
                }
            }
            .animation(.easeInOut(duration: 0.18), value: model.toast)
        }
        .preferredColorScheme(.dark)
        .task {
            if model.items.isEmpty {
                model.showCatalog(title: "首页", path: "/")
            }
        }
        .fullScreenCover(item: $model.player) { request in
            PlayerScreen(
                request: request,
                onProgress: { position, duration in
                    model.finishPlayback(episode: request.episode, position: position, duration: duration)
                },
                onClose: { position, duration in
                    model.finishPlayback(episode: request.episode, position: position, duration: duration)
                }
            )
        }
    }

    @ViewBuilder
    private func content(metrics: Metrics) -> some View {
        switch model.route {
        case .catalog:
            CatalogScreen(model: model, metrics: metrics)
        case .detail:
            DetailScreen(model: model, metrics: metrics)
        case .search:
            SearchScreen(model: model, metrics: metrics)
        case .settings:
            SettingsScreen(model: model, metrics: metrics)
        }
    }
}
