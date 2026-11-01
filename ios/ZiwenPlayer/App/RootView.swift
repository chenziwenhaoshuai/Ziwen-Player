import SwiftUI

struct RootView: View {
    @StateObject private var model = AppModel()

    var body: some View {
        TabView {
            Tab("首页", systemImage: "house.fill") {
                NavigationStack { HomeScreen(model: model) }
            }
            Tab("搜索", systemImage: "magnifyingglass") {
                NavigationStack { SearchScreen(model: model) }
            }
            Tab("最近", systemImage: "clock.arrow.circlepath") {
                NavigationStack { RecentScreen(model: model) }
            }
            Tab("设置", systemImage: "gearshape.fill") {
                NavigationStack { SettingsScreen(model: model) }
            }
        }
        .tint(Theme.accent)
        .overlay(alignment: .top) {
            if let toast = model.toast {
                ToastBanner(text: toast)
                    .padding(.top, 6)
                    .allowsHitTesting(false)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.2), value: model.toast)
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
        .task {
            if model.items.isEmpty {
                model.select(AppModel.categories[0])
            }
        }
    }
}
