import SwiftUI

struct SearchScreen: View {
    @ObservedObject var model: AppModel
    @FocusState private var focused: Bool

    var body: some View {
        ZStack {
            AppBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    searchBar

                    if model.searching {
                        ProgressView()
                            .tint(Theme.accent)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 60)
                    } else if model.searchResults.isEmpty {
                        EmptyStateView(
                            icon: "magnifyingglass",
                            title: "输入关键词后按搜索",
                            subtitle: "支持片名、演员"
                        )
                    } else {
                        LazyVGrid(columns: Layout.posterColumns, spacing: 16) {
                            ForEach(model.searchResults) { item in
                                NavigationLink(value: item) {
                                    VideoCard(item: item)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
                .padding(.bottom, 96)
            }
            .scrollDismissesKeyboard(.immediately)
        }
        .navigationTitle("搜索")
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(for: VideoItem.self) { item in
            DetailScreen(model: model, video: item)
        }
    }

    private var searchBar: some View {
        HStack(spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Theme.muted2)

                TextField("片名、演员或关键词", text: $model.searchKeyword)
                    .textFieldStyle(.plain)
                    .focused($focused)
                    .submitLabel(.search)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                    .foregroundColor(Theme.text)
                    .onSubmit { model.performSearch(model.searchKeyword) }
            }
            .padding(.horizontal, 14)
            .frame(height: 44)
            .glassEffect(.regular, in: .capsule)

            Button {
                focused = false
                model.performSearch(model.searchKeyword)
            } label: {
                Text("搜索")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.black)
                    .padding(.horizontal, 18)
                    .frame(height: 44)
            }
            .buttonStyle(.plain)
            .glassEffect(.regular.tint(Theme.accent).interactive(), in: .capsule)
        }
        .padding(.horizontal, 16)
        .padding(.top, 6)
    }
}
