import Foundation
import Testing
@testable import PanelixDesktop

struct PanelixDesktopTests {
    @Test func naturalSortOrdersNumberedPages() {
        let pages = ["page10.jpg", "page2.jpg", "page1.jpg"].sorted(by: panelixNaturalSort)
        #expect(pages == ["page1.jpg", "page2.jpg", "page10.jpg"])
    }

    @Test func comicFormatDetectionMatchesAndroidFormats() {
        #expect(URL(fileURLWithPath: "/Comics/Book.PDF").panelixFormat == .pdf)
        #expect(URL(fileURLWithPath: "/Comics/Book.zip").panelixFormat == .cbz)
        #expect(URL(fileURLWithPath: "/Comics/Book.rar").panelixFormat == .cbr)
        #expect(URL(fileURLWithPath: "/Comics/Book.tar").panelixFormat == .cbt)
        #expect(URL(fileURLWithPath: "/Comics/Book.7z").panelixFormat == .cb7)
    }

    @Test func scannerIdIsStableForSameURL() {
        let url = URL(fileURLWithPath: "/tmp/Panelix/Test.cbz")
        #expect(ComicScanner.id(for: url) == ComicScanner.id(for: url))
    }
}
