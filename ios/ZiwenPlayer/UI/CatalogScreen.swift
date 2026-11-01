import SwiftUI

struct VideoCard: View {
    let item: VideoItem
    let metrics: Metrics

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            RemoteImage(url: item.poster, cornerRadius: 8)
                .aspectRatio(2.0 / 3.0, contentMode: .fit)
                .frame(maxWidth: .infinity)

            Text(item.title)
                .font(.system(size: metrics.videoTitleSize, weight: .semibold))
                .foregroundColor(Theme.text)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text(item.displayMeta.isEmpty ? item.remarks : item.displayMeta)
                .font(.system(size: metrics.videoMetaSize))
                .foregroundColor(Theme.muted)
                .lineLimit(1)
        }
        .padding(metrics.cardPadding)
        .background(Theme.panel)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .contentShape(Rectangle())
    }
}

struct CatalogScreen: View {
    @ObservedObject var model: AppModel
    let metrics: Metrics

    private var columns: [GridItem] {
        [GridItem(.adaptive(minimum: 110, maximum: 180), spacing: 12)]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(model.title)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(Theme.text)
                Spacer()
                if model.isPeachSelected {
                    Button("换一批") { model.refreshPeachRandomly() }
                        .buttonStyle(PillButtonStyle())
                }
            }
            .padding(.horizontal, metrics.pageHorizontalPadding)
            .padding(.top, metrics.pageTopPadding)

            ScrollView {
                if model.items.isEmpty {
                    if model.loading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: Theme.accent))
                            .frame(maxWidth: .infinity)
                            .padding(.top, 60)
                    } else {
                        Text("暂无内容")
                            .foregroundColor(Theme.muted)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 60)
                    }
                } else {
                    LazyVGrid(columns: columns, spacing: 14) {
                        ForEach(model.items) { item in
                            VideoCard(item: item, metrics: metrics)
                                .onAppear { model.loadMoreIfNeeded(current: item) }
                                .onTapGesture { model.openDetail(item) }
                        }
                    }
                    .padding(.horizontal, metrics.pageHorizontalPadding)
                    .padding(.bottom, metrics.pageBottomPadding)
                }
            }
            .refreshable {
                if model.isPeachSelected {
                    model.refreshPeachRandomly()
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}
