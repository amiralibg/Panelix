import Foundation
import SwiftUI

enum ComicFormat: String, Codable, CaseIterable, Identifiable {
    case pdf, cbz, cbr, cbt, cb7, folder, unknown

    var id: String { rawValue }
    var label: String { rawValue.uppercased() }
}

enum ReaderLayoutMode: String, Codable, CaseIterable, Identifiable {
    case vertical, horizontal, spread
    var id: String { rawValue }
    var label: String { rawValue.capitalized }
}

enum ReadingDirection: String, Codable, CaseIterable, Identifiable {
    case ltr, rtl
    var id: String { rawValue }
    var label: String { self == .ltr ? "Left to Right" : "Right to Left" }
}

enum ThemePreference: String, Codable, CaseIterable, Identifiable {
    case system, light, dark
    var id: String { rawValue }
    var label: String { rawValue.capitalized }
}

enum LibraryViewMode: String, Codable, CaseIterable, Identifiable {
    case grid, compact, list
    var id: String { rawValue }
}

enum AccentColor: String, Codable, CaseIterable, Identifiable {
    case coral, teal, violet, amber
    var id: String { rawValue }
    var label: String { rawValue.capitalized }
}

enum SortOption: String, Codable, CaseIterable, Identifiable {
    case title, recentlyAdded, recentlyOpened
    var id: String { rawValue }

    var label: String {
        switch self {
        case .title: "A-Z"
        case .recentlyAdded: "Recently Added"
        case .recentlyOpened: "Recently Opened"
        }
    }
}

struct FolderRecord: Identifiable, Codable, Hashable {
    var id: UUID = UUID()
    var url: URL
    var bookmarkData: Data?
    var name: String
    var createdAt: Date = Date()
}

struct ComicRecord: Identifiable, Codable, Hashable {
    var id: String
    var url: URL
    var folderURL: URL
    var title: String
    var format: ComicFormat
    var coverURL: URL?
    var pageCount: Int?
    var fileSize: Int64?
    var addedAt: Date
    var updatedAt: Date
    var lastOpenedAt: Date?
    var isAvailable: Bool
    var parserMessage: String?
    var sourceModifiedAt: Date?
}

struct ReadingProgress: Codable, Hashable {
    var comicId: String
    var currentPage: Int
    var totalPages: Int
    var updatedAt: Date = Date()
}

struct BookmarkRecord: Identifiable, Codable, Hashable {
    var id: UUID = UUID()
    var comicId: String
    var page: Int
    var note: String?
    var createdAt: Date = Date()
    var updatedAt: Date = Date()
}

struct ComicReaderPreferences: Codable, Hashable {
    var comicId: String
    var readerLayoutMode: ReaderLayoutMode
    var readingDirection: ReadingDirection
    var brightness: Double
    var contrast: Double
    var updatedAt: Date = Date()
}

struct AppPreferences: Codable, Hashable {
    var themePreference: ThemePreference = .system
    var libraryViewMode: LibraryViewMode = .grid
    var sortOption: SortOption = .recentlyAdded
    var readerLayoutMode: ReaderLayoutMode = .horizontal
    var readingDirection: ReadingDirection = .ltr
    var hasCompletedOnboarding: Bool = false
    var accentColor: AccentColor = .coral
    var showProgressOnCovers: Bool = true
}

struct ScanProgress: Hashable {
    var completed: Int
    var total: Int
    var currentTitle: String?

    var fraction: Double {
        total <= 0 ? 0 : min(max(Double(completed) / Double(total), 0), 1)
    }
}

struct ReaderPage: Identifiable, Hashable {
    var id: Int { index }
    var index: Int
    var url: URL
}

struct ReaderDocument {
    var pageCount: Int
    var pages: [ReaderPage]
    var parserMessage: String?
}

extension String {
    var panelixExtension: String { (self as NSString).pathExtension.lowercased() }
    var panelixTitle: String { ((self as NSString).deletingPathExtension as String).isEmpty ? self : (self as NSString).deletingPathExtension }
    var isPanelixImageName: Bool { ["jpg", "jpeg", "png", "webp", "bmp", "gif", "tiff", "heic"].contains(panelixExtension) }
}

extension URL {
    var panelixDisplayName: String { lastPathComponent.isEmpty ? path : lastPathComponent }
    var panelixFormat: ComicFormat {
        switch panelixDisplayName.panelixExtension {
        case "pdf": .pdf
        case "cbz", "zip": .cbz
        case "cbr", "rar": .cbr
        case "cbt", "tar": .cbt
        case "cb7", "7z": .cb7
        default: .unknown
        }
    }
}

let panelixNaturalSort: (String, String) -> Bool = { left, right in
    left.localizedStandardCompare(right) == .orderedAscending
}
