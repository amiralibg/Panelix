import SwiftUI

extension AccentColor {
    var color: Color {
        switch self {
        case .coral:  Color(red: 1.00,  green: 0.478, blue: 0.349) // #FF7A59
        case .teal:   Color(red: 0.204, green: 0.784, blue: 0.671) // #34C8AB
        case .violet: Color(red: 0.655, green: 0.545, blue: 0.980) // #A78BFA
        case .amber:  Color(red: 0.961, green: 0.694, blue: 0.239) // #F5B13D
        }
    }
}

private struct PanelixAccentColorKey: EnvironmentKey {
    static let defaultValue: Color = AccentColor.coral.color
}

extension EnvironmentValues {
    var panelixAccent: Color {
        get { self[PanelixAccentColorKey.self] }
        set { self[PanelixAccentColorKey.self] = newValue }
    }
}
