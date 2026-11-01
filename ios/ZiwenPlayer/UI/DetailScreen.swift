import SwiftUI

struct DetailScreen: View {
    @ObservedObject var model: AppModel
    let metrics: Metrics

    private var columns: [GridItem] {
        [GridItem(.adaptive(minimum: 78, maximum: 120), spacing: metrics.detailEpisodeSpacing)]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 14) {
                Button {
                    model.closeDetail()
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text("返回")
                    }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(Theme.text)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(Theme.panel)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)

                Text(model.detail?.title ?? model.selectedItem?.title ?? "详情")
                    .font(.system(size: metrics.detailTitleSize, weight: .bold))
                    .foregroundColor(Theme.text)
                    .lineLimit(1)

                Spacer()
            }
            .padding(.horizontal, metrics.detailHorizontalPadding)
            .padding(.top, metrics.detailVerticalPadding)

            if model.detailLoading {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: Theme.accent))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let detail = model.detail {
                ScrollView {
                    HStack(alignment: .top, spacing: 18) {
                        RemoteImage(url: detail.poster, cornerRadius: 10)
                            .frame(width: metrics.detailPosterWidth, height: metrics.detailPosterHeight)

                        VStack(alignment: .leading, spacing: 10) {
                            if !detail.meta.isEmpty {
                                Text(detail.meta)
                                    .font(.system(size: metrics.detailMetaSize))
                                    .foregroundColor(Theme.muted)
                            }
                            Text(detail.description)
                                .font(.system(size: metrics.detailDescSize))
                                .foregroundColor(Theme.muted)
                                .lineSpacing(2)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(.horizontal, metrics.detailHorizontalPadding)

                    let groups = detail.sourceGroups
                    if groups.count > 1 {
                        SectionTitle(text: "线路来源", size: metrics.detailSectionTitleSize)
                            .padding(.horizontal, metrics.detailHorizontalPadding)
                            .padding(.top, 16)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(groups) { group in
                                    Button {
                                        model.activeSource = group.source
                                    } label: {
                                        Text(group.title)
                                            .font(.system(size: metrics.detailSourceTextSize, weight: .semibold))
                                            .foregroundColor(model.activeSource == group.source ? .black : Theme.text)
                                            .padding(.horizontal, 14)
                                            .padding(.vertical, 8)
                                            .background(model.activeSource == group.source ? Theme.accent : Theme.panel)
                                            .clipShape(Capsule())
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(.horizontal, metrics.detailHorizontalPadding)
                        }
                    }

                    SectionTitle(text: "剧集", size: metrics.detailSectionTitleSize)
                        .padding(.horizontal, metrics.detailHorizontalPadding)
                        .padding(.top, 16)

                    LazyVGrid(columns: columns, spacing: metrics.detailEpisodeSpacing) {
                        ForEach(model.activeEpisodes) { episode in
                            Button {
                                model.play(episode)
                            } label: {
                                Text(episode.title)
                                    .font(.system(size: metrics.detailEpisodeTextSize, weight: .semibold))
                                    .foregroundColor(Theme.text)
                                    .lineLimit(1)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: metrics.detailEpisodeCellHeight)
                                    .padding(.horizontal, 4)
                                    .background(Theme.panel)
                                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, metrics.detailHorizontalPadding)
                    .padding(.bottom, metrics.detailVerticalPadding)
                }
            } else {
                Text("加载失败，请返回重试")
                    .foregroundColor(Theme.muted)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}
