import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var store: LibraryStore

    var body: some View {
        Group {
            if !store.preferences.hasCompletedOnboarding {
                OnboardingView()
            } else {
                NavigationSplitView {
                    LibrarySidebar()
                        .navigationSplitViewColumnWidth(min: 210, ideal: 240, max: 280)
                } detail: {
                    if let id = store.selectedComicId, let comic = store.comic(id: id) {
                        ReaderView(comic: comic)
                    } else if store.showingSettings {
                        SettingsView()
                    } else {
                        LibraryView()
                    }
                }
            }
        }
        .preferredColorScheme(colorScheme)
    }

    private var colorScheme: ColorScheme? {
        switch store.preferences.themePreference {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

// MARK: - Onboarding

private struct OnboardingView: View {
    @EnvironmentObject private var store: LibraryStore
    @Environment(\.panelixAccent) private var accent

    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            ZStack {
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(LinearGradient(colors: [accent, accent.opacity(0.6)],
                                        startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 90, height: 90)
                    .shadow(color: accent.opacity(0.4), radius: 18, y: 8)
                Image(systemName: "books.vertical.fill")
                    .font(.system(size: 42, weight: .medium))
                    .foregroundStyle(.white)
            }

            VStack(spacing: 8) {
                Text("Welcome to Panelix")
                    .font(.largeTitle.bold())
                Text("Your comic reader for PDF, CBZ, CBR and more.\nAdd a folder to get started.")
                    .font(.title3)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }

            VStack(spacing: 16) {
                stepRow(1, title: "Add a folder",
                        description: "Point Panelix at a folder containing your comics.")
                stepRow(2, title: "Browse your library",
                        description: "Comics are scanned and organised automatically.")
                stepRow(3, title: "Start reading",
                        description: "Pick up where you left off with Continue Reading.")
            }
            .frame(maxWidth: 440)

            Button {
                store.addFolder()
            } label: {
                Label("Add Comic Folder", systemImage: "folder.badge.plus")
                    .font(.headline)
                    .frame(width: 220)
                    .padding(.vertical, 4)
            }
            .buttonStyle(.borderedProminent)
            .tint(accent)
            .controlSize(.large)

            Spacer()
        }
        .padding(40)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func stepRow(_ number: Int, title: String, description: String) -> some View {
        HStack(alignment: .top, spacing: 16) {
            Text("\(number)")
                .font(.headline.bold())
                .foregroundStyle(accent)
                .frame(width: 30, height: 30)
                .background(accent.opacity(0.14), in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline)
                Text(description).font(.callout).foregroundStyle(.secondary)
            }
            Spacer()
        }
    }
}

// MARK: - Sidebar

private struct LibrarySidebar: View {
    @EnvironmentObject private var store: LibraryStore
    @Environment(\.panelixAccent) private var accent

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(spacing: 10) {
                if let appIcon = NSImage(named: NSImage.applicationIconName) {
                    Image(nsImage: appIcon)
                        .resizable()
                        .frame(width: 38, height: 38)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text("Panelix")
                        .font(.headline.bold())
                    Text("\(store.comics.count) comics")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.top, 10)

            VStack(spacing: 4) {
                SidebarNavButton(title: "Library", systemImage: "square.grid.2x2.fill",
                                 isSelected: !store.showingSettings && store.selectedComicId == nil) {
                    store.showingSettings = false
                    store.selectedComicId = nil
                }
                SidebarNavButton(title: "Settings", systemImage: "gearshape.fill",
                                 isSelected: store.showingSettings) {
                    store.showingSettings = true
                    store.selectedComicId = nil
                }
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Folders")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .textCase(.uppercase)
                if store.folders.isEmpty {
                    Text("No folders yet")
                        .font(.callout)
                        .foregroundStyle(.tertiary)
                } else {
                    ForEach(store.folders) { folder in
                        Label(folder.name, systemImage: "folder")
                            .font(.callout)
                            .lineLimit(1)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Spacer()

            VStack(spacing: 8) {
                Button { store.addFolder() } label: {
                    Label("Add Folder", systemImage: "plus")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(accent)

                Button { store.rescanAll() } label: {
                    Label("Rescan", systemImage: "arrow.clockwise")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(.ultraThinMaterial)
    }
}

private struct SidebarNavButton: View {
    @Environment(\.panelixAccent) private var accent
    let title: String
    let systemImage: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .font(.callout.weight(.semibold))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .background(isSelected ? accent.opacity(0.15) : Color.clear,
                            in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .foregroundStyle(isSelected ? accent : .primary)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Library

private struct LibraryView: View {
    @EnvironmentObject private var store: LibraryStore
    @Environment(\.panelixAccent) private var accent

    private static let visibleFilters: [ComicFormat] = [.pdf, .cbz, .cbr]

    var body: some View {
        VStack(spacing: 0) {
            libraryHeader
            controls
            if store.isScanning { ScanProgressView(progress: store.scanProgress) }
            if store.visibleComics.isEmpty && !store.isScanning {
                EmptyLibraryState(query: store.query)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        if !store.continueReading.isEmpty {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Continue Reading")
                                    .font(.title2.bold())
                                    .padding(.horizontal)
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 14) {
                                        ForEach(store.continueReading) { comic in
                                            ContinueReadingCard(
                                                comic: comic,
                                                progress: store.progress[comic.id]
                                            ) { store.openComic(comic) }
                                        }
                                    }
                                    .padding(.horizontal)
                                }
                            }
                            Divider().padding(.horizontal)
                        }
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Library")
                                .font(.title2.bold())
                                .padding(.horizontal)
                            comicCollection(store.visibleComics)
                                .padding(.horizontal)
                        }
                    }
                    .padding(.vertical)
                }
            }
        }
        .navigationTitle("Panelix")
        .alert("Panelix", isPresented: Binding(
            get: { store.errorMessage != nil },
            set: { if !$0 { store.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) {}
        } message: { Text(store.errorMessage ?? "") }
        .onAppear {
            if let filter = store.filter, !Self.visibleFilters.contains(filter) {
                store.filter = nil
            }
        }
    }

    private var libraryHeader: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Library")
                    .font(.largeTitle.bold())
                Text("\(store.visibleComics.count) shown • \(store.comics.count) total")
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if store.isScanning {
                ProgressView().controlSize(.small)
            }
        }
        .padding(.horizontal)
        .padding(.top, 20)
        .padding(.bottom, 4)
    }

    private var controls: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                searchField
                Picker("Sort", selection: $store.preferences.sortOption) {
                    ForEach(SortOption.allCases) { Text($0.label).tag($0) }
                }
                .labelsHidden()
                .frame(width: 150)
                .onChange(of: store.preferences.sortOption) { _, _ in store.save() }
                Picker("View", selection: $store.preferences.libraryViewMode) {
                    Image(systemName: "square.grid.2x2").tag(LibraryViewMode.grid)
                    Image(systemName: "square.grid.3x3").tag(LibraryViewMode.compact)
                    Image(systemName: "list.bullet").tag(LibraryViewMode.list)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                .frame(width: 108)
                .onChange(of: store.preferences.libraryViewMode) { _, _ in store.save() }
            }
            HStack(spacing: 8) {
                filterPill("All", nil)
                ForEach(Self.visibleFilters) { format in
                    filterPill(format.label, format)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding()
    }

    private var searchField: some View {
        HStack(spacing: 6) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
                .font(.callout)
            TextField("Search", text: $store.query)
                .textFieldStyle(.plain)
            if !store.query.isEmpty {
                Button { store.query = "" } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 5)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
        .frame(maxWidth: .infinity)
    }

    private func filterPill(_ label: String, _ format: ComicFormat?) -> some View {
        let isActive = store.filter == format
        return Button(label) { store.filter = format }
            .buttonStyle(.plain)
            .font(.callout.weight(isActive ? .semibold : .regular))
            .padding(.horizontal, 12)
            .padding(.vertical, 5)
            .background(isActive ? accent : Color.clear, in: Capsule())
            .foregroundStyle(isActive ? .white : .secondary)
            .overlay(isActive ? nil : Capsule().strokeBorder(Color.secondary.opacity(0.3), lineWidth: 1))
            .animation(.easeInOut(duration: 0.15), value: isActive)
    }

    @ViewBuilder
    private func comicCollection(_ comics: [ComicRecord]) -> some View {
        switch store.preferences.libraryViewMode {
        case .grid:
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 184, maximum: 184), spacing: 16)], spacing: 16) {
                ForEach(comics) { comic in
                    ComicCard(comic: comic, progress: store.progress[comic.id],
                              showProgress: store.preferences.showProgressOnCovers) {
                        store.openComic(comic)
                    }
                }
            }
        case .compact:
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 134, maximum: 134), spacing: 12)], spacing: 12) {
                ForEach(comics) { comic in
                    ComicCardCompact(comic: comic, progress: store.progress[comic.id],
                                     showProgress: store.preferences.showProgressOnCovers) {
                        store.openComic(comic)
                    }
                }
            }
        case .list:
            LazyVStack(spacing: 10) {
                ForEach(comics) { comic in
                    ComicRow(comic: comic, progress: store.progress[comic.id],
                             showProgress: store.preferences.showProgressOnCovers) {
                        store.openComic(comic)
                    }
                }
            }
        }
    }
}

// MARK: - Continue Reading Card

private struct ContinueReadingCard: View {
    @Environment(\.panelixAccent) private var accent
    let comic: ComicRecord
    let progress: ReadingProgress?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 6) {
                CoverView(url: comic.coverURL, format: comic.format, title: comic.title)
                    .frame(width: 110, height: 162)
                    .overlay(alignment: .bottom) {
                        progressBar(progress)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .shadow(color: .black.opacity(0.20), radius: 10, y: 6)
                Text(comic.title)
                    .font(.caption.weight(.semibold))
                    .lineLimit(2)
                    .frame(width: 110, alignment: .leading)
                    .foregroundStyle(.primary)
            }
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func progressBar(_ p: ReadingProgress?) -> some View {
        if let p, p.totalPages > 1 {
            ProgressView(value: Double(p.currentPage), total: Double(p.totalPages - 1))
                .progressViewStyle(.linear)
                .tint(accent)
                .padding(.horizontal, 6)
                .padding(.bottom, 6)
        }
    }
}

// MARK: - Comic Cards

private struct ComicCard: View {
    @Environment(\.panelixAccent) private var accent
    let comic: ComicRecord
    let progress: ReadingProgress?
    let showProgress: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                CoverView(url: comic.coverURL, format: comic.format, title: comic.title)
                    .frame(width: 160, height: 236)
                    .overlay(alignment: .bottom) {
                        if showProgress, let p = progress, p.totalPages > 1 {
                            ProgressView(value: Double(p.currentPage), total: Double(p.totalPages - 1))
                                .progressViewStyle(.linear)
                                .tint(accent)
                                .padding(.horizontal, 6)
                                .padding(.bottom, 6)
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .shadow(color: .black.opacity(0.20), radius: 12, y: 8)
                Text(comic.title)
                    .font(.headline.weight(.semibold))
                    .frame(height: 40, alignment: .topLeading)
                    .lineLimit(2)
                    .foregroundStyle(.primary)
                HStack {
                    Text(comic.format.label).font(.caption.bold())
                    if let pages = comic.pageCount { Text("\(pages) pages").font(.caption) }
                    if !comic.isAvailable {
                        Image(systemName: "exclamationmark.triangle").foregroundStyle(.orange)
                    }
                }
                .foregroundStyle(.secondary)
                .frame(height: 18, alignment: .leading)
            }
            .padding(12)
            .frame(width: 184, height: 330, alignment: .top)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(.white.opacity(0.08), lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
    }
}

private struct ComicCardCompact: View {
    @Environment(\.panelixAccent) private var accent
    let comic: ComicRecord
    let progress: ReadingProgress?
    let showProgress: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 6) {
                CoverView(url: comic.coverURL, format: comic.format, title: comic.title)
                    .frame(width: 118, height: 174)
                    .overlay(alignment: .bottom) {
                        if showProgress, let p = progress, p.totalPages > 1 {
                            ProgressView(value: Double(p.currentPage), total: Double(p.totalPages - 1))
                                .progressViewStyle(.linear)
                                .tint(accent)
                                .padding(.horizontal, 4)
                                .padding(.bottom, 4)
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .shadow(color: .black.opacity(0.18), radius: 8, y: 5)
                Text(comic.title)
                    .font(.caption.weight(.semibold))
                    .lineLimit(2)
                    .foregroundStyle(.primary)
            }
            .padding(8)
            .frame(width: 134, alignment: .top)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(.white.opacity(0.08), lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
    }
}

private struct ComicRow: View {
    @Environment(\.panelixAccent) private var accent
    let comic: ComicRecord
    let progress: ReadingProgress?
    let showProgress: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                CoverView(url: comic.coverURL, format: comic.format, title: comic.title)
                    .frame(width: 72, height: 106)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                VStack(alignment: .leading, spacing: 4) {
                    Text(comic.title).font(.headline)
                    Text("\(comic.format.label) • \(comic.pageCount.map(String.init) ?? "?") pages")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if let message = comic.parserMessage {
                        Text(message).font(.caption).foregroundStyle(.orange)
                    }
                    if showProgress, let p = progress, p.totalPages > 1 {
                        HStack(spacing: 8) {
                            ProgressView(value: Double(p.currentPage), total: Double(p.totalPages - 1))
                                .progressViewStyle(.linear)
                                .tint(accent)
                            Text("\(p.currentPage)/\(p.totalPages)")
                                .font(.caption.monospacedDigit())
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                Spacer()
            }
            .padding(12)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Shared Components

private struct CoverView: View {
    let url: URL?
    let format: ComicFormat
    let title: String

    var body: some View {
        ZStack {
            if let url, let image = NSImage(contentsOf: url) {
                Image(nsImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                PlaceholderCover(format: format, title: title)
            }
        }
        .clipped()
    }
}

private struct PlaceholderCover: View {
    let format: ComicFormat
    let title: String

    var body: some View {
        ZStack {
            LinearGradient(colors: placeholderColors, startPoint: .topLeading, endPoint: .bottomTrailing)
            VStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 34, weight: .medium))
                    .foregroundStyle(.white.opacity(0.78))
                Text(format.label)
                    .font(.caption.bold())
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(.black.opacity(0.24), in: Capsule())
                Text(title)
                    .font(.caption.weight(.semibold))
                    .multilineTextAlignment(.center)
                    .lineLimit(3)
                    .padding(.horizontal, 10)
            }
            .foregroundStyle(.white.opacity(0.90))
            .padding(12)
        }
    }

    private var placeholderColors: [Color] {
        switch format {
        case .cbr:    [.purple.opacity(0.75), .red.opacity(0.70), .black.opacity(0.65)]
        case .cbz:    [.blue.opacity(0.75), .cyan.opacity(0.55), .black.opacity(0.65)]
        case .pdf:    [.red.opacity(0.85), .orange.opacity(0.55), .black.opacity(0.65)]
        case .folder: [.indigo.opacity(0.75), .mint.opacity(0.50), .black.opacity(0.65)]
        default:      [.gray.opacity(0.70), .black.opacity(0.65)]
        }
    }

    private var icon: String {
        switch format {
        case .cbr, .cbz, .cb7, .cbt: "archivebox.fill"
        case .pdf:                    "doc.richtext.fill"
        case .folder:                 "folder.fill"
        default:                      "book.closed.fill"
        }
    }
}

private struct EmptyLibraryState: View {
    let query: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: query.isEmpty ? "books.vertical" : "magnifyingglass")
                .font(.system(size: 42, weight: .medium))
                .foregroundStyle(.secondary)
            Text(query.isEmpty ? "No comics found" : "No matches")
                .font(.title2.bold())
            Text(query.isEmpty
                 ? "Add a folder or rescan after placing PDF, CBZ, or CBR files in your library."
                 : "No comics match \"\(query)\".")
                .font(.callout)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 420)
        }
        .padding(.top, 54)
        .padding(.horizontal, 24)
    }
}

private struct ScanProgressView: View {
    let progress: ScanProgress?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ProgressView(value: progress?.fraction ?? 0)
            HStack {
                Text(progress.map { "Scanning \($0.completed)/\($0.total)" } ?? "Counting comics...")
                Spacer()
                Text(progress?.currentTitle ?? "")
            }
            .font(.caption)
            .foregroundStyle(.secondary)
        }
        .padding(.horizontal)
        .padding(.bottom, 8)
    }
}
