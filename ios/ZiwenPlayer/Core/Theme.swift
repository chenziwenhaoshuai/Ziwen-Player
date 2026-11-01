import SwiftUI

enum Theme {
    static let bg = Color(hex: 0x08080B)
    static let bgSoft = Color(hex: 0x0D0D12)
    static let panel = Color(hex: 0x121218)
    static let panel2 = Color(hex: 0x17171F)
    static let line = Color(hex: 0x26262F)
    static let text = Color(hex: 0xF3F3F6)
    static let muted = Color(hex: 0x9A9AAB)
    static let muted2 = Color(hex: 0x6F6F7E)
    static let accent = Color(hex: 0xF7C843)
    static let danger = Color(hex: 0xFF4D5E)
}

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: 1.0
        )
    }
}

enum Layout {
    /// 海报 2:3，宽度自适应
    static let posterColumns = [GridItem(.adaptive(minimum: 108, maximum: 168), spacing: 12)]
    static let episodeColumns = [GridItem(.adaptive(minimum: 76, maximum: 112), spacing: 10)]
}

extension View {
    /// 深色卡片：面板色 + 细描边
    func cardBackground(corner: CGFloat = 14) -> some View {
        background(
            RoundedRectangle(cornerRadius: corner, style: .continuous).fill(Theme.panel)
        )
        .overlay(
            RoundedRectangle(cornerRadius: corner, style: .continuous)
                .stroke(Theme.line, lineWidth: 1)
        )
    }

    /// iOS 26 Liquid Glass
    func glassCard(corner: CGFloat = 18) -> some View {
        glassEffect(.regular, in: .rect(cornerRadius: corner))
    }
}

struct AppBackground: View {
    var body: some View {
        ZStack {
            Theme.bg
            LinearGradient(
                colors: [Theme.accent.opacity(0.10), .clear, Color(hex: 0x1B1030).opacity(0.5)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
        .ignoresSafeArea()
    }
}
