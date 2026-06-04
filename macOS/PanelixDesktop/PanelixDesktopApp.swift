import SwiftUI

@main
struct PanelixDesktopApp: App {
    @StateObject private var store = LibraryStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(store)
                .environment(\.panelixAccent, store.preferences.accentColor.color)
        }
        .commands {
            CommandGroup(after: .newItem) {
                Button("Add Comic Folder") { store.addFolder() }
                    .keyboardShortcut("o", modifiers: [.command])
                Button("Rescan Library") { store.rescanAll() }
                    .keyboardShortcut("r", modifiers: [.command])
            }
        }
    }
}
