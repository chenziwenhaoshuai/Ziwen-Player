import SwiftUI

enum Theme {
    static let bg = Color(hex: 0x101318)
    static let panel = Color(hex: 0x181E27)
    static let panel2 = Color(hex: 0x222A35)
    static let text = Color.white
    static let muted = Color(hex: 0xAFB8C6)
    static let accent = Color(hex: 0xF7C843)
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

/// 按屏幕高度划分的三档尺寸，对应 Android 端的 tiny / compact / regular。
struct Metrics {
    let compact: Bool
    let tiny: Bool

    init(screenHeight: CGFloat) {
        tiny = screenHeight < 420
        compact = screenHeight < 540
    }

    var pageHorizontalPadding: CGFloat { tiny ? 16 : (compact ? 22 : 32) }
    var pageTopPadding: CGFloat { tiny ? 8 : (compact ? 12 : 24) }
    var pageBottomPadding: CGFloat { tiny ? 8 : (compact ? 10 : 18) }

    var navWidth: CGFloat { tiny ? 138 : (compact ? 152 : 170) }
    var brandTextSize: CGFloat { tiny ? 20 : (compact ? 22 : 26) }
    var navItemHeight: CGFloat { tiny ? 34 : (compact ? 42 : 54) }
    var navTextSize: CGFloat { tiny ? 15 : (compact ? 16 : 18) }
    var navHorizontalPadding: CGFloat { tiny ? 14 : 20 }

    var posterHeight: CGFloat { tiny ? 128 : (compact ? 154 : 190) }
    var videoTitleSize: CGFloat { tiny ? 13 : (compact ? 14 : 15) }
    var videoMetaSize: CGFloat { tiny ? 10 : (compact ? 11 : 12) }
    var cardPadding: CGFloat { tiny ? 4 : (compact ? 5 : 7) }

    var detailHorizontalPadding: CGFloat { tiny ? 14 : (compact ? 20 : 36) }
    var detailVerticalPadding: CGFloat { tiny ? 8 : (compact ? 14 : 28) }
    var detailTitleSize: CGFloat { tiny ? 20 : (compact ? 23 : 28) }
    var detailPosterWidth: CGFloat { tiny ? 118 : (compact ? 164 : 260) }
    var detailPosterHeight: CGFloat { tiny ? 172 : (compact ? 238 : 378) }
    var detailMetaSize: CGFloat { tiny ? 12 : (compact ? 14 : 18) }
    var detailDescSize: CGFloat { tiny ? 12 : (compact ? 14 : 17) }
    var detailSectionTitleSize: CGFloat { tiny ? 15 : (compact ? 18 : 22) }
    var detailEpisodeTextSize: CGFloat { tiny ? 13 : (compact ? 15 : 17) }
    var detailEpisodeCellHeight: CGFloat { tiny ? 36 : (compact ? 44 : 54) }
    var detailEpisodeSpacing: CGFloat { tiny ? 6 : (compact ? 8 : 12) }
    var detailSourceTextSize: CGFloat { tiny ? 12 : (compact ? 14 : 16) }

    /// 按宽度自适应列数，保持海报 2:3 比例
    var catalogColumns: [GridItem] {
        [GridItem(.adaptive(minimum: 108, maximum: 200), spacing: 12)]
    }
}
