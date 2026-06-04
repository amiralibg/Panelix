import AppKit
import Foundation
import PDFKit
import CryptoKit

final class ComicScanner {
    private let parser = ComicParserService()

    func scan(folder: FolderRecord, existing: [ComicRecord], onProgress: @escaping (ScanProgress) -> Void) async throws -> [ComicRecord] {
        let root = folder.url
        let candidates = collectCandidates(in: root)
        var completed = 0
        onProgress(ScanProgress(completed: completed, total: candidates.count, currentTitle: root.lastPathComponent))
        let oldById = Dictionary(uniqueKeysWithValues: existing.map { ($0.id, $0) })
        var found: [ComicRecord] = []

        for candidate in candidates {
            let old = oldById[candidate.id]
            let metadata = parser.parseForLibrary(url: candidate.url, format: candidate.format, old: old)
            let now = Date()
            found.append(ComicRecord(
                id: candidate.id,
                url: candidate.url,
                folderURL: root,
                title: candidate.url.panelixDisplayName.panelixTitle,
                format: candidate.format,
                coverURL: metadata.coverURL ?? old?.coverURL,
                pageCount: metadata.pageCount ?? old?.pageCount,
                fileSize: candidate.fileSize,
                addedAt: old?.addedAt ?? now,
                updatedAt: now,
                lastOpenedAt: old?.lastOpenedAt,
                isAvailable: metadata.parserMessage == nil,
                parserMessage: metadata.parserMessage,
                sourceModifiedAt: candidate.modifiedAt
            ))
            completed += 1
            onProgress(ScanProgress(completed: completed, total: candidates.count, currentTitle: candidate.url.lastPathComponent))
        }
        return found
    }

    private func collectCandidates(in root: URL) -> [ComicCandidate] {
        let resourceKeys: Set<URLResourceKey> = [.isDirectoryKey, .isRegularFileKey, .fileSizeKey, .contentModificationDateKey]
        guard let enumerator = FileManager.default.enumerator(at: root, includingPropertiesForKeys: Array(resourceKeys), options: [.skipsHiddenFiles]) else { return [] }
        var candidates: [ComicCandidate] = []
        var imageFolders = Set<URL>()

        for case let url as URL in enumerator {
            guard let values = try? url.resourceValues(forKeys: resourceKeys) else { continue }
            if values.isDirectory == true {
                let images = directImages(in: url)
                if images.count >= 3, !imageFolders.contains(url) {
                    imageFolders.insert(url)
                    candidates.append(candidate(url: url, root: root, format: .folder, values: values))
                }
            } else if values.isRegularFile == true {
                let format = url.panelixFormat
                if format != .unknown {
                    candidates.append(candidate(url: url, root: root, format: format, values: values))
                }
            }
        }
        return candidates.sorted { panelixNaturalSort($0.url.path, $1.url.path) }
    }

    private func directImages(in folder: URL) -> [URL] {
        (try? FileManager.default.contentsOfDirectory(at: folder, includingPropertiesForKeys: [.isRegularFileKey], options: [.skipsHiddenFiles]))?
            .filter { $0.lastPathComponent.isPanelixImageName }
            .sorted { panelixNaturalSort($0.lastPathComponent, $1.lastPathComponent) } ?? []
    }

    private func candidate(url: URL, root: URL, format: ComicFormat, values: URLResourceValues) -> ComicCandidate {
        ComicCandidate(
            id: Self.id(for: url),
            url: url,
            format: format,
            fileSize: Int64(values.fileSize ?? 0),
            modifiedAt: values.contentModificationDate
        )
    }

    static func id(for url: URL) -> String {
        let data = Data(url.path.utf8)
        return Insecure.SHA1.hash(data: data).map { String(format: "%02x", $0) }.joined()
    }
}

private struct ComicCandidate {
    var id: String
    var url: URL
    var format: ComicFormat
    var fileSize: Int64?
    var modifiedAt: Date?
}

struct LibraryMetadata {
    var coverURL: URL?
    var pageCount: Int?
    var parserMessage: String?
}

final class ComicParserService {
    private let cache = PanelixCache()

    func parseForLibrary(url: URL, format: ComicFormat, old: ComicRecord?) -> LibraryMetadata {
        if old?.fileSize == fileSize(url), old?.sourceModifiedAt == modifiedAt(url), old?.coverURL != nil {
            return LibraryMetadata(coverURL: nil, pageCount: nil, parserMessage: old?.parserMessage)
        }
        switch format {
        case .pdf: return parsePDF(url)
        case .cbz: return parseCBZ(url)
        case .folder: return parseImageFolder(url)
        case .cbr: return parseArchive(url, format: .cbr)
        case .cbt: return LibraryMetadata(coverURL: nil, pageCount: nil, parserMessage: "CBT detected but is not supported yet")
        case .cb7: return LibraryMetadata(coverURL: nil, pageCount: nil, parserMessage: "CB7 detected but is not supported yet")
        case .unknown: return LibraryMetadata(coverURL: nil, pageCount: nil, parserMessage: "Unsupported comic format")
        }
    }

    func loadForReader(_ comic: ComicRecord) -> ReaderDocument {
        switch comic.format {
        case .pdf: return loadPDF(comic.url)
        case .cbz: return loadCBZ(comic.url)
        case .folder: return loadImageFolder(comic.url)
        case .cbr: return loadArchive(comic.url, format: .cbr)
        case .cbt: return ReaderDocument(pageCount: 0, pages: [], parserMessage: "CBT detected but is not supported yet")
        case .cb7: return ReaderDocument(pageCount: 0, pages: [], parserMessage: "CB7 detected but is not supported yet")
        case .unknown: return ReaderDocument(pageCount: 0, pages: [], parserMessage: "Unsupported comic format")
        }
    }

    private func parsePDF(_ url: URL) -> LibraryMetadata {
        guard let document = PDFDocument(url: url), document.pageCount > 0 else {
            return LibraryMetadata(coverURL: nil, pageCount: nil, parserMessage: "PDF could not render")
        }
        let cover = cache.coverURL(for: url, ext: "jpg")
        if let page = document.page(at: 0), let image = page.thumbnail(of: CGSize(width: 600, height: 900), for: .mediaBox).panelixJPEGData(compressionQuality: 0.9) {
            try? image.write(to: cover)
        }
        return LibraryMetadata(coverURL: cover, pageCount: document.pageCount, parserMessage: nil)
    }

    private func loadPDF(_ url: URL) -> ReaderDocument {
        guard let document = PDFDocument(url: url), document.pageCount > 0 else {
            return ReaderDocument(pageCount: 0, pages: [], parserMessage: "PDF could not render")
        }
        let pageDir = cache.pageDirectory(for: url)
        let pages = (0..<document.pageCount).compactMap { index -> ReaderPage? in
            let output = pageDir.appendingPathComponent(String(format: "%05d.jpg", index))
            if !FileManager.default.fileExists(atPath: output.path), let page = document.page(at: index) {
                let image = page.thumbnail(of: CGSize(width: 1400, height: 1800), for: .mediaBox)
                try? image.panelixJPEGData(compressionQuality: 0.92)?.write(to: output)
            }
            return ReaderPage(index: index, url: output)
        }
        return ReaderDocument(pageCount: document.pageCount, pages: pages, parserMessage: nil)
    }

    private func parseCBZ(_ url: URL) -> LibraryMetadata {
        parseArchive(url, format: .cbz)
    }

    private func parseArchive(_ url: URL, format: ComicFormat) -> LibraryMetadata {
        let extraction = extractedArchiveDirectory(for: url, format: format)
        guard extraction.message == nil else {
            return LibraryMetadata(coverURL: nil, pageCount: nil, parserMessage: extraction.message)
        }
        let images = recursiveImages(in: extraction.url)
        guard let first = images.first else { return LibraryMetadata(coverURL: nil, pageCount: nil, parserMessage: "Archive has no readable image pages") }
        let cover = cache.coverURL(for: url, ext: first.lastPathComponent.panelixExtension.isEmpty ? "jpg" : first.lastPathComponent.panelixExtension)
        if !FileManager.default.fileExists(atPath: cover.path) {
            try? FileManager.default.copyItem(at: first, to: cover)
        }
        return LibraryMetadata(coverURL: cover, pageCount: images.count, parserMessage: nil)
    }

    private func loadCBZ(_ url: URL) -> ReaderDocument {
        loadArchive(url, format: .cbz)
    }

    private func loadArchive(_ url: URL, format: ComicFormat) -> ReaderDocument {
        let extraction = extractedArchiveDirectory(for: url, format: format)
        guard extraction.message == nil else {
            return ReaderDocument(pageCount: 0, pages: [], parserMessage: extraction.message)
        }
        let images = recursiveImages(in: extraction.url)
        let pages = images.enumerated().map { ReaderPage(index: $0.offset, url: $0.element) }
        return ReaderDocument(pageCount: pages.count, pages: pages, parserMessage: nil)
    }

    private func parseImageFolder(_ url: URL) -> LibraryMetadata {
        let images = imageFiles(in: url)
        guard images.count >= 3 else { return LibraryMetadata(coverURL: images.first, pageCount: images.count, parserMessage: "Image folder needs at least 3 readable image files") }
        return LibraryMetadata(coverURL: images.first, pageCount: images.count, parserMessage: nil)
    }

    private func loadImageFolder(_ url: URL) -> ReaderDocument {
        let images = imageFiles(in: url)
        return ReaderDocument(pageCount: images.count, pages: images.enumerated().map { ReaderPage(index: $0.offset, url: $0.element) }, parserMessage: nil)
    }

    private func imageFiles(in url: URL) -> [URL] {
        (try? FileManager.default.contentsOfDirectory(at: url, includingPropertiesForKeys: [.isRegularFileKey], options: [.skipsHiddenFiles]))?
            .filter { $0.lastPathComponent.isPanelixImageName }
            .sorted { panelixNaturalSort($0.lastPathComponent, $1.lastPathComponent) } ?? []
    }

    private func extractedArchiveDirectory(for url: URL, format: ComicFormat) -> (url: URL, message: String?) {
        let output = cache.pageDirectory(for: url).appendingPathComponent("archive-\(format.rawValue)", isDirectory: true)
        if FileManager.default.fileExists(atPath: output.path), !recursiveImages(in: output).isEmpty {
            return (output, nil)
        }
        try? FileManager.default.removeItem(at: output)
        try? FileManager.default.createDirectory(at: output, withIntermediateDirectories: true)

        let commands = archiveCommands(for: url, output: output, format: format)
        var lastMessage: String?
        for command in commands {
            let process = Process()
            process.executableURL = URL(fileURLWithPath: command.executable)
            process.arguments = command.arguments
            do {
                try process.run()
                process.waitUntilExit()
                if process.terminationStatus == 0, !recursiveImages(in: output).isEmpty {
                    return (output, nil)
                }
                lastMessage = "\(format.label) could not be extracted with \(URL(fileURLWithPath: command.executable).lastPathComponent)"
            } catch {
                lastMessage = error.localizedDescription
            }
        }
        return (output, lastMessage ?? "\(format.label) could not be extracted")
    }

    private func archiveCommands(for url: URL, output: URL, format: ComicFormat) -> [ArchiveCommand] {
        switch format {
        case .cbz:
            return [
                ArchiveCommand(executable: "/usr/bin/ditto", arguments: ["-x", "-k", url.path, output.path]),
                ArchiveCommand(executable: "/usr/bin/bsdtar", arguments: ["-xf", url.path, "-C", output.path]),
            ]
        case .cbr:
            return existingExecutables([
                ArchiveCommand(executable: "/opt/homebrew/bin/7zz", arguments: ["x", "-y", "-o\(output.path)", url.path]),
                ArchiveCommand(executable: "/usr/local/bin/7zz", arguments: ["x", "-y", "-o\(output.path)", url.path]),
                ArchiveCommand(executable: "/opt/homebrew/bin/unar", arguments: ["-quiet", "-force-overwrite", "-output-directory", output.path, url.path]),
                ArchiveCommand(executable: "/usr/local/bin/unar", arguments: ["-quiet", "-force-overwrite", "-output-directory", output.path, url.path]),
                ArchiveCommand(executable: "/usr/bin/bsdtar", arguments: ["-xf", url.path, "-C", output.path]),
            ])
        default:
            return []
        }
    }

    private func existingExecutables(_ commands: [ArchiveCommand]) -> [ArchiveCommand] {
        commands.filter { FileManager.default.isExecutableFile(atPath: $0.executable) }
    }

    private func recursiveImages(in url: URL) -> [URL] {
        guard let enumerator = FileManager.default.enumerator(at: url, includingPropertiesForKeys: [.isRegularFileKey], options: [.skipsHiddenFiles]) else { return [] }
        return enumerator.compactMap { $0 as? URL }
            .filter { $0.lastPathComponent.isPanelixImageName }
            .sorted { panelixNaturalSort($0.path, $1.path) }
    }

    private func fileSize(_ url: URL) -> Int64? { Int64((try? url.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0) }
    private func modifiedAt(_ url: URL) -> Date? { try? url.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate }
}

private struct ArchiveCommand {
    var executable: String
    var arguments: [String]
}

final class PanelixCache {
    private let root: URL

    init() {
        root = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!.appendingPathComponent("Panelix", isDirectory: true)
        try? FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
    }

    func coverURL(for source: URL, ext: String) -> URL {
        let folder = root.appendingPathComponent("Covers", isDirectory: true)
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        return folder.appendingPathComponent("\(ComicScanner.id(for: source)).\(ext)")
    }

    func pageDirectory(for source: URL) -> URL {
        let folder = root.appendingPathComponent("Pages", isDirectory: true).appendingPathComponent(ComicScanner.id(for: source), isDirectory: true)
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        return folder
    }
}
private extension NSImage {
    func panelixJPEGData(compressionQuality: CGFloat) -> Data? {
        guard let tiffRepresentation, let bitmap = NSBitmapImageRep(data: tiffRepresentation) else { return nil }
        return bitmap.representation(using: .jpeg, properties: [.compressionFactor: compressionQuality])
    }
}
