import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: LibraryStore
    @Environment(\.panelixAccent) private var accent

    var body: some View {
        Form {
            Section("Appearance") {
                Picker("Theme", selection: $store.preferences.themePreference) {
                    ForEach(ThemePreference.allCases) { Text($0.label).tag($0) }
                }
                accentColorPicker
                Toggle("Show progress on covers", isOn: $store.preferences.showProgressOnCovers)
            }
            Section("Default Reader") {
                Picker("Layout", selection: $store.preferences.readerLayoutMode) {
                    ForEach(ReaderLayoutMode.allCases) { Text($0.label).tag($0) }
                }
                Picker("Direction", selection: $store.preferences.readingDirection) {
                    ForEach(ReadingDirection.allCases) { Text($0.label).tag($0) }
                }
            }
            Section("Library Folders") {
                Button("Add Folder", systemImage: "plus") { store.addFolder() }
                Button("Rescan Library", systemImage: "arrow.clockwise") { store.rescanAll() }
                ForEach(store.folders) { folder in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(folder.name)
                            Text(folder.url.path)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                        Spacer()
                        Button("Remove", role: .destructive) { store.removeFolder(folder) }
                    }
                }
            }
            Section("Format Support") {
                Text("PDF, CBZ/ZIP, image folders, and CBR/RAR are readable when a local extractor such as 7zz or unar is installed. CBT/TAR and CB7/7z are detected and shown with unsupported-state messaging until native extractor integrations are added.")
                    .foregroundStyle(.secondary)
            }
        }
        .formStyle(.grouped)
        .navigationTitle("Settings")
        .onChange(of: store.preferences) { _, _ in store.save() }
    }

    private var accentColorPicker: some View {
        HStack {
            Text("Accent Color")
            Spacer()
            HStack(spacing: 10) {
                ForEach(AccentColor.allCases) { color in
                    Button {
                        store.preferences.accentColor = color
                    } label: {
                        ZStack {
                            Circle()
                                .fill(color.color)
                                .frame(width: 26, height: 26)
                            if store.preferences.accentColor == color {
                                Circle()
                                    .strokeBorder(.white.opacity(0.8), lineWidth: 2)
                                    .frame(width: 26, height: 26)
                                Image(systemName: "checkmark")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundStyle(.white)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                    .help(color.label)
                    .animation(.easeInOut(duration: 0.15), value: store.preferences.accentColor)
                }
            }
        }
    }
}
