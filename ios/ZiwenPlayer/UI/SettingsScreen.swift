import SwiftUI

struct SettingsScreen: View {
    @ObservedObject var model: AppModel
    let metrics: Metrics

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("设置")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(Theme.text)
                .padding(.horizontal, metrics.pageHorizontalPadding)
                .padding(.top, metrics.pageTopPadding)

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    section("播放") {
                        HStack(spacing: 12) {
                            Text("提前缓冲：\(model.preloadMinutes) 分钟")
                                .font(.system(size: 15))
                                .foregroundColor(Theme.text)
                            Spacer()
                            Button("减少") { adjust(-1) }
                                .buttonStyle(PillButtonStyle())
                            Button("增加") { adjust(1) }
                                .buttonStyle(PillButtonStyle())
                        }
                        caption("缓冲越多越流畅，也越费流量。范围 1 / 2 / 3 / 5 / 8 分钟。")
                    }

                    section("最近观看") {
                        Button("清除历史浏览记录") { model.clearHistory() }
                            .buttonStyle(PillButtonStyle())
                        caption("只保存在本机，共最多保留 \(Storage.recentLimit) 条。")
                    }

                    section("内测模式") {
                        Button(model.betaMode ? "内测模式：已开启" : "内测模式：未开启") {
                            model.toggleBetaMode()
                        }
                        .buttonStyle(PillButtonStyle(filled: model.betaMode))
                        caption("开启后会显示额外的分类入口。")
                    }

                    section("关于") {
                        Text("当前版本：\(model.version)")
                            .font(.system(size: 14))
                            .foregroundColor(Theme.muted)
                        caption("iOS 移植版 · SwiftUI + AVPlayer。仅供学习使用。")
                    }
                }
                .padding(.horizontal, metrics.pageHorizontalPadding)
                .padding(.bottom, metrics.pageBottomPadding)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private func adjust(_ delta: Int) {
        let options = Storage.preloadOptions
        let index = options.firstIndex(of: model.preloadMinutes) ?? 2
        let next = min(max(index + delta, 0), options.count - 1)
        model.setPreloadMinutes(options[next])
    }

    @ViewBuilder
    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Theme.accent)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Theme.panel)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func caption(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 12))
            .foregroundColor(Theme.muted)
    }
}
