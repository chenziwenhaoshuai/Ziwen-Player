import SwiftUI

struct SettingsScreen: View {
    @ObservedObject var model: AppModel

    var body: some View {
        ZStack {
            AppBackground()

            ScrollView {
                VStack(spacing: 14) {
                    card(title: "播放") {
                        HStack {
                            Text("提前缓冲：\(model.preloadMinutes) 分钟")
                                .font(.system(size: 14))
                                .foregroundColor(Theme.text)
                            Spacer()
                            Button("减少") { adjust(-1) }
                                .buttonStyle(GlassPillButton())
                            Button("增加") { adjust(1) }
                                .buttonStyle(GlassPillButton())
                        }
                        caption("缓冲越多越流畅，也越费流量。可选 1 / 2 / 3 / 5 / 8 分钟。")
                    }

                    card(title: "最近观看") {
                        HStack {
                            Text("共 \(model.recent.count) 条记录")
                                .font(.system(size: 14))
                                .foregroundColor(Theme.text)
                            Spacer()
                            Button("清除") { model.clearHistory() }
                                .buttonStyle(GlassPillButton(tint: Theme.danger))
                        }
                        caption("只保存在本机，最多保留 \(Storage.recentLimit) 条。")
                    }

                    card(title: "内测模式") {
                        Button {
                            model.toggleBetaMode()
                        } label: {
                            HStack {
                                Text(model.betaMode ? "已开启" : "未开启")
                                    .font(.system(size: 14, weight: .semibold))
                                Spacer()
                                Image(systemName: model.betaMode ? "checkmark.circle.fill" : "circle")
                            }
                            .foregroundColor(model.betaMode ? Theme.accent : Theme.text)
                            .padding(.horizontal, 14)
                            .frame(height: 44)
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.plain)
                        .glassEffect(
                            model.betaMode ? .regular.tint(Theme.accent.opacity(0.35)).interactive() : .regular.interactive(),
                            in: .capsule
                        )
                        caption("开启后会显示额外的分类入口。")
                    }

                    card(title: "关于") {
                        infoRow("版本", model.version)
                        infoRow("引擎", "SwiftUI + AVPlayer")
                        caption("iOS 移植版，仅供学习使用。")
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 96)
            }
        }
        .navigationTitle("设置")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func adjust(_ delta: Int) {
        let options = Storage.preloadOptions
        let index = options.firstIndex(of: model.preloadMinutes) ?? 2
        let next = min(max(index + delta, 0), options.count - 1)
        model.setPreloadMinutes(options[next])
    }

    @ViewBuilder
    private func card<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Theme.accent)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .cardBackground(corner: 18)
    }

    private func infoRow(_ name: String, _ value: String) -> some View {
        HStack {
            Text(name)
                .font(.system(size: 13))
                .foregroundColor(Theme.muted)
            Spacer()
            Text(value)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(Theme.text)
        }
    }

    private func caption(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11))
            .foregroundColor(Theme.muted2)
    }
}

struct GlassPillButton: ButtonStyle {
    var tint: Color? = nil

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 13, weight: .semibold))
            .foregroundColor(tint ?? Theme.text)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .glassEffect(
                tint == nil ? .regular.interactive() : .regular.tint(tint!.opacity(0.3)).interactive(),
                in: .capsule
            )
            .opacity(configuration.isPressed ? 0.6 : 1)
    }
}
