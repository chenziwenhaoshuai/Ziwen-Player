import SwiftUI

struct SidebarView: View {
    @ObservedObject var model: AppModel
    let metrics: Metrics

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("子文播放器")
                .font(.system(size: metrics.brandTextSize, weight: .heavy))
                .foregroundColor(Theme.text)
                .padding(.horizontal, metrics.navHorizontalPadding)
                .frame(height: max(36, metrics.navItemHeight + 10))

            ScrollView(showsIndicators: false) {
                VStack(spacing: 6) {
                    navButton("⌕  搜索", active: model.route == .search) {
                        model.openSearch()
                    }

                    ForEach(model.visibleCategories) { category in
                        navButton(category.name, active: isActive(category)) {
                            model.showCatalog(title: category.name, path: category.path)
                        }
                        if category.path == "/" {
                            navButton("最近观看", active: isRecentActive) {
                                model.showRecentWatches()
                            }
                        }
                    }

                    navButton("设置", active: model.route == .settings) {
                        model.route = .settings
                    }
                }
                .padding(.horizontal, 10)
            }

            VStack(alignment: .leading, spacing: 6) {
                Text("如果喜欢请扫码赞助我")
                    .font(.system(size: 11))
                    .foregroundColor(Theme.muted)
                Text("仅供学习使用")
                    .font(.system(size: 10))
                    .foregroundColor(Theme.muted.opacity(0.6))
            }
            .padding(.horizontal, metrics.navHorizontalPadding)
            .padding(.bottom, 14)
            .padding(.top, 8)
        }
        .frame(width: metrics.navWidth, alignment: .leading)
        .frame(maxHeight: .infinity, alignment: .top)
        .background(Theme.bg)
    }

    private var isRecentActive: Bool {
        model.route == .catalog && model.title == "最近观看"
    }

    private func isActive(_ category: Category) -> Bool {
        guard model.route == .catalog, model.title != "最近观看" else { return false }
        return model.path == category.path
    }

    @ViewBuilder
    private func navButton(_ title: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: metrics.navTextSize, weight: active ? .bold : .regular))
                .foregroundColor(active ? Theme.accent : Theme.text)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, metrics.navHorizontalPadding)
                .frame(height: metrics.navItemHeight)
                .background(active ? Theme.panel2 : Theme.panel)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}
