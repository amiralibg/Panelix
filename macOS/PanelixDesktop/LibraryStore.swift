import Combine
import Foundation
import SwiftUI

final class LibraryStore: ObservableObject {
    @Published var folders: [FolderRecord] = []
    @Published var comics: [ComicRecord] = []
    @Published var progress: [String: ReadingProgress] = [:]
    @Published var bookmarks: [BookmarkRecord] = []
    @Published var readerPreferences: [String: ComicReaderPreferences] = [:]
    @Published var preferences = AppPreferences()
    @Published var query = ""
    @Published var filter: ComicFormat?
    @Published var isScanning = false
    @Published var scanProgress: ScanProgress?
    @Published var errorMessage: String?
    @Published var selectedComicId: String?
    @Published var showingSettings = false

    private let scanner = ComicScanner()
    private let persistenceURL: URL

    init() {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
            .appendingPathComponent("Panelix", isDirectory: true)
        try? FileManager.default.createDirectory(at: base, withIntermediateDirectories: true)
        persistenceURL = base.appendingPathComponent("Library.json")
        load()
    }

    var visibleComics: [ComicRecord] {
        comics
            .filter { query.isEmpty || $0.title.localizedCaseInsensitiveContains(query) }
            .filter { filter == nil || $0.format == filter }
            .sorted { first, second in
                switch preferences.sortOption {
                case .title: panelixNaturalSort(first.title, second.title)
                case .recentlyAdded: first.addedAt > second.addedAt
                case .recentlyOpened: (first.lastOpenedAt ?? .distantPast) > (second.lastOpenedAt ?? .distantPast)
                }
            }
    }

    var continueReading: [ComicRecord] {
        comics
            .filter { (progress[$0.id]?.currentPage ?? 0) > 0 }
            .sorted { (progress[$0.id]?.updatedAt ?? .distantPast) > (progress[$1.id]?.updatedAt ?? .distantPast) }
            .prefix(8)
            .map { $0 }
    }

    func addFolder() {
        let panel = NSOpenPanel()
        panel.canChooseDirectories = true
        panel.canChooseFiles = false
        panel.allowsMultipleSelection = false
        panel.prompt = "Add Comic Folder"
        guard panel.runModal() == .OK, let url = panel.url else { return }
        let bookmark = try? url.bookmarkData(options: [.withSecurityScope], includingResourceValuesForKeys: nil, relativeTo: nil)
        if !folders.contains(where: { $0.url == url }) {
            folders.append(FolderRecord(url: url, bookmarkData: bookmark, name: url.lastPathComponent))
        }
        preferences.hasCompletedOnboarding = true
        save()
        rescanAll()
    }

    func removeFolder(_ folder: FolderRecord) {
        folders.removeAll { $0.id == folder.id }
        comics.removeAll { $0.folderURL == folder.url }
        save()
    }

    func rescanAll() {
        guard !isScanning else { return }
        isScanning = true
        scanProgress = ScanProgress(completed: 0, total: 0, currentTitle: "Counting comics")
        errorMessage = nil
        Task {
            do {
                var results: [ComicRecord] = []
                for folder in folders {
                    results += try await scanner.scan(folder: resolvedFolder(folder), existing: comics.filter { $0.folderURL == folder.url }) { [weak self] progress in
                        self?.scanProgress = progress
                    }
                }
                comics = mergeScanned(results)
                save()
            } catch {
                errorMessage = error.localizedDescription
            }
            isScanning = false
            scanProgress = nil
        }
    }

    func openComic(_ comic: ComicRecord) {
        if let index = comics.firstIndex(where: { $0.id == comic.id }) {
            comics[index].lastOpenedAt = Date()
        }
        selectedComicId = comic.id
        save()
    }

    func saveProgress(comicId: String, page: Int, totalPages: Int) {
        progress[comicId] = ReadingProgress(comicId: comicId, currentPage: max(0, page), totalPages: totalPages)
        save()
    }

    func toggleBookmark(comicId: String, page: Int) {
        if let existing = bookmarks.firstIndex(where: { $0.comicId == comicId && $0.page == page }) {
            bookmarks.remove(at: existing)
        } else {
            bookmarks.append(BookmarkRecord(comicId: comicId, page: page, note: nil))
        }
        save()
    }

    func setReaderPreferences(_ value: ComicReaderPreferences) {
        readerPreferences[value.comicId] = value
        save()
    }

    func readerPreferences(for comicId: String) -> ComicReaderPreferences? { readerPreferences[comicId] }
    func comic(id: String) -> ComicRecord? { comics.first { $0.id == id } }
    func bookmarks(for comicId: String) -> [BookmarkRecord] { bookmarks.filter { $0.comicId == comicId }.sorted { $0.page < $1.page } }

    private func resolvedFolder(_ folder: FolderRecord) -> FolderRecord {
        guard let data = folder.bookmarkData else { return folder }
        var stale = false
        if let url = try? URL(resolvingBookmarkData: data, options: [.withSecurityScope], relativeTo: nil, bookmarkDataIsStale: &stale) {
            _ = url.startAccessingSecurityScopedResource()
            var resolved = folder
            resolved.url = url
            return resolved
        }
        return folder
    }

    private func mergeScanned(_ scanned: [ComicRecord]) -> [ComicRecord] {
        let scannedIds = Set(scanned.map(\.id))
        let missing = comics.filter { !scannedIds.contains($0.id) }.map { comic in
            var copy = comic
            copy.isAvailable = false
            copy.parserMessage = "Source file is no longer available"
            copy.updatedAt = Date()
            return copy
        }
        return scanned + missing
    }

    private func load() {
        guard let data = try? Data(contentsOf: persistenceURL), let state = try? JSONDecoder().decode(PersistedState.self, from: data) else { return }
        folders = state.folders
        comics = state.comics
        progress = Dictionary(uniqueKeysWithValues: state.progress.map { ($0.comicId, $0) })
        bookmarks = state.bookmarks
        readerPreferences = Dictionary(uniqueKeysWithValues: state.readerPreferences.map { ($0.comicId, $0) })
        preferences = state.preferences
    }

    func save() {
        let state = PersistedState(
            folders: folders,
            comics: comics,
            progress: Array(progress.values),
            bookmarks: bookmarks,
            readerPreferences: Array(readerPreferences.values),
            preferences: preferences
        )
        if let data = try? JSONEncoder().encode(state) {
            try? data.write(to: persistenceURL, options: [.atomic])
        }
    }
}

private struct PersistedState: Codable {
    var folders: [FolderRecord]
    var comics: [ComicRecord]
    var progress: [ReadingProgress]
    var bookmarks: [BookmarkRecord]
    var readerPreferences: [ComicReaderPreferences]
    var preferences: AppPreferences
}
