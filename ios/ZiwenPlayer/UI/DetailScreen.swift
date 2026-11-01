import SwiftUI

struct DetailScreen: View {
    @ObservedObject var model: AppModel
    let video: VideoItem

    @StateObject private var engine = PlaybackEngine()

    var body: some View {
        ZStack {
            AppBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if model.detailLoading {
                        ProgressView()
                            .tint(Theme.accent)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 80)
                    } else if let detail = model.detail {
                        if engine.isActive {
                            InlinePlayerContainer(engine: engine) {
                                engine.finish()
                            }
                        } else {
                            header(detail)
                        }

                        if !detail.description.isEmpty {
                            description(detail.description)
                        }
                        if model.activeGroups.count > 1 {
                            sourceSection
                        }
                        episodeSection
                    } else {
                        EmptyStateView(icon: "exclamationmark.triangle", title: "加载失败", subtitle: "返回上一页重试")
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 96)
            }
        }
        .navigationTitle(video.title)
        .navigationBarTitleDisplayMode(.inline)
        .task(id: video.url) {
            await model.loadDetail(video)
        }
        .onDisappear {
            engine.finish()
        }
    }

    // MARK: - 播放

    private func start(_ episode: Episode) {
        Task {
            guard let request = await model.resolve(episode) else { return }

            if request.target.isDirectVideo {
                engine.onProgress = { position, duration in
                    model.finishPlayback(episode: episode, position: position, duration: duration)
                }
                engine.load(target: request.target, episode: episode, resume: request.resume)
            } else {
                // 需要 WebView 抓流的线路，退回全屏播放页
                model.player = AppModel.PlayerRequest(
                    target: request.target,
                    episode: episode,
                    resume: request.resume
                )
            }
        }
    }

    // MARK: - 头部

    private func header(_ detail: VideoDetail) -> some View {
        HStack(alignment: .top, spacing: 14) {
            RemoteImage(url: detail.poster, cornerRadius: 14)
                .frame(width: 118, height: 177)

            VStack(alignment: .leading, spacing: 8) {
                Text(detail.title)
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(Theme.text)
                    .lineLimit(2)

                if !detail.meta.isEmpty {
                    Text(detail.meta)
                        .font(.system(size: 12))
                        .foregroundColor(Theme.muted)
                        .lineLimit(3)
                }

                HStack(spacing: 6) {
                    infoPill("\(detail.episodes.count) 集")
                    infoPill("\(detail.sourceGroups.count) 条线路")
                }
                .padding(.top, 2)

                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .cardBackground(corner: 18)
    }

    private func infoPill(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .medium))
            .foregroundColor(Theme.muted)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(Theme.panel2)
            .clipShape(Capsule())
    }

    // MARK: - 简介

    private func description(_ text: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("剧情简介")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Theme.accent)
            Text(text)
                .font(.system(size: 13))
                .foregroundColor(Theme.muted)
                .lineSpacing(3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .cardBackground(corner: 18)
    }

    // MARK: - 线路

    private var sourceSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("线路来源")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Theme.accent)

            ScrollView(.horizontal, showsIndicators: false) {
                GlassEffectContainer(spacing: 8) {
                    HStack(spacing: 8) {
                        ForEach(model.activeGroups) { group in
                            CategoryChip(
                                title: group.title,
                                active: model.activeSource == group.source
                            ) {
                                model.activeSource = group.source
                            }
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .cardBackground(corner: 18)
    }

    // MARK: - 剧集

    private var episodeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("剧集")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(Theme.accent)
                Spacer()
                Text("\(model.activeEpisodes.count) 集")
                    .font(.system(size: 11))
                    .foregroundColor(Theme.muted2)
            }

            LazyVGrid(columns: Layout.episodeColumns, spacing: 10) {
                ForEach(model.activeEpisodes) { episode in
                    Button {
                        start(episode)
                    } label: {
                        Text(episode.title)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(engine.episode?.id == episode.id ? .black : Theme.text)
                            .lineLimit(1)
                            .frame(maxWidth: .infinity)
                            .frame(height: 40)
                            .background(
                                engine.episode?.id == episode.id ? Theme.accent : Theme.panel2
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .cardBackground(corner: 18)
    }
}
