import SwiftUI

struct SearchScreen: View {
    @ObservedObject var model: AppModel
    let metrics: Metrics

    private var columns: [GridItem] {
        [GridItem(.adaptive(minimum: 110, maximum: 180), spacing: 12)]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                TextField("输入片名、演员或关键词", text: $model.searchKeyword)
                    .textFieldStyle(.plain)
                    .submitLabel(.search)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                    .foregroundColor(Theme.text)
                    .padding(.horizontal, 16)
                    .frame(height: 44)
                    .background(Theme.panel)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .onSubmit { model.performSearch(model.searchKeyword) }

                Button("搜索") { model.performSearch(model.searchKeyword) }
                    .buttonStyle(PillButtonStyle(filled: true))
                    .frame(height: 44)
            }
            .padding(.horizontal, metrics.pageHorizontalPadding)
            .padding(.top, metrics.pageTopPadding)

            if model.searching {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: Theme.accent))
                    .frame(maxWidth: .infinity)
                    .padding(.top, 40)
            } else if model.searchResults.isEmpty {
                Text("输入关键词后按搜索")
                    .foregroundColor(Theme.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 40)
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 14) {
                        ForEach(model.searchResults) { item in
                            VideoCard(item: item, metrics: metrics)
                                .onTapGesture { model.openDetail(item) }
                        }
                    }
                    .padding(.horizontal, metrics.pageHorizontalPadding)
                    .padding(.bottom, metrics.pageBottomPadding)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}
