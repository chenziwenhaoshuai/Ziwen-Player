import SwiftUI

struct HomeScreen: View {
    @ObservedObject var model: AppModel

    var body: some View {
        ZStack {
            AppBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    categoryBar

                    if model.loading && model.items.isEmpty {
                        ProgressView()
                            .tint(Theme.accent)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 80)
                    } else if model.items.isEmpty {
                        EmptyStateView(icon: "film.stack", title: "暂无内容", subtitle: "下拉试试，或换一个分类")
                    } else {
                        LazyVGrid(columns: Layout.posterColumns, spacing: 16) {
                            ForEach(model.items) { item in
                                NavigationLink(value: item) {
                                    VideoCard(item: item)
                                }
                                .buttonStyle(.plain)
                                .onAppear { model.loadMoreIfNeeded(current: item) }
                            }
                        }
                        .padding(.horizontal, 16)

                        if model.loadingMore {
                            ProgressView()
                                .tint(Theme.accent)
                                .padding(.vertical, 18)
                        }
                    }
                }
                .padding(.bottom, 96)
            }
            .refreshable {
                if model.isPeachSelected {
                    model.refreshPeachRandomly()
                }
            }
        }
        .navigationTitle(model.title)
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(for: VideoItem.self) { item in
            DetailScreen(model: model, video: item)
        }
    }

    private var categoryBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            GlassEffectContainer(spacing: 8) {
                HStack(spacing: 8) {
                    ForEach(model.visibleCategories) { category in
                        CategoryChip(
                            title: category.name,
                            active: model.path == category.path
                        ) {
                            model.select(category)
                        }
                    }

                    if model.isPeachSelected {
                        Button {
                            model.refreshPeachRandomly()
                        } label: {
                            Label("换一批", systemImage: "shuffle")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(Theme.text)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                        }
                        .buttonStyle(.plain)
                        .glassEffect(.regular.interactive(), in: .capsule)
                    }
                }
                .padding(.horizontal, 16)
            }
            .padding(.vertical, 6)
        }
    }
}
