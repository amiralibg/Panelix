import SwiftUI

struct ReaderView: View {
    @EnvironmentObject private var store: LibraryStore
    @Environment(\.panelixAccent) private var accent
    let comic: ComicRecord
    @State private var document = ReaderDocument(pageCount: 0, pages: [], parserMessage: nil)
    @State private var currentPage = 0
    @State private var isLoading = true
    @State private var showControls = true
    @State private var scale: CGFloat = 1
    @State private var pageOffset: CGSize = .zero
    @State private var scrollAccumulator: CGFloat = 0
    @State private var didPageDuringScroll = false
    @State private var showBookmarks = false
    @State private var showTune = false
    private let parser = ComicParserService()

    private var prefs: ComicReaderPreferences {
        store.readerPreferences(for: comic.id) ?? ComicReaderPreferences(
            comicId: comic.id,
            readerLayoutMode: store.preferences.readerLayoutMode,
            readingDirection: store.preferences.readingDirection,
            brightness: 0,
            contrast: 1
        )
    }

    var body: some View {
        ZStack {
            Color.clear.ignoresSafeArea()
            content
            ReaderInputView(
                isZoomed: scale > 1.01,
                onMagnify: { amount in
                    scale = (scale + amount).clamped(to: 0.65...5)
                    if scale <= 1.01 {
                        scale = 1
                        pageOffset = .zero
                    }
                },
                onSmartMagnify: {
                    withAnimation(.snappy) {
                        if scale > 1 {
                            scale = 1
                            pageOffset = .zero
                        } else {
                            scale = 2.2
                        }
                    }
                },
                onHorizontalScroll: handleHorizontalScroll,
                onScrollEnded: resetHorizontalScrollGesture,
                onPan: { delta in
                    pageOffset = CGSize(
                        width: pageOffset.width + delta.width,
                        height: pageOffset.height + delta.height
                    )
                },
                onPreviousPage: goToPreviousPage,
                onNextPage: goToNextPage
            )
            if showControls { overlay }
        }
        .background(Color.black.opacity(0.001))
        .foregroundStyle(.primary)
        .task { load() }
        .onTapGesture { withAnimation { showControls.toggle() } }
        .onChange(of: currentPage) { _, page in
            store.saveProgress(comicId: comic.id, page: page, totalPages: document.pageCount)
        }
    }

    @ViewBuilder
    private var content: some View {
        if isLoading {
            ProgressView("Preparing pages...").tint(.white)
        } else if let message = document.parserMessage, document.pages.isEmpty {
            ReaderMessageView(title: "Can't Open Comic", message: message)
        } else if document.pages.isEmpty {
            ReaderMessageView(title: "No Pages Found",
                              message: "Panelix could not prepare any readable pages for this comic.")
        } else if prefs.readerLayoutMode == .vertical {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(document.pages) { page in pageImage(page).id(page.index) }
                    }
                    .padding(.vertical)
                }
                .onAppear { proxy.scrollTo(currentPage, anchor: .center) }
            }
        } else {
            GeometryReader { proxy in
                ZStack {
                    if prefs.readerLayoutMode == .spread,
                       currentPage + 1 < document.pages.count,
                       proxy.size.width > 900 {
                        HStack(spacing: 10) {
                            pageImage(document.pages[currentPage])
                            pageImage(document.pages[currentPage + 1])
                        }
                        .padding(.horizontal, 18)
                    } else if let page = document.pages[safe: currentPage] {
                        pageImage(page)
                            .padding(.horizontal, 18)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }

    private func pageImage(_ page: ReaderPage) -> some View {
        Image(nsImage: NSImage(contentsOf: page.url) ?? NSImage())
            .resizable()
            .scaledToFit()
            .scaleEffect(scale)
            .offset(pageOffset)
            .brightness(prefs.brightness)
            .contrast(prefs.contrast)
            .onTapGesture(count: 2) {
                withAnimation(.snappy) {
                    if scale > 1 {
                        scale = 1
                        pageOffset = .zero
                    } else {
                        scale = 2
                    }
                }
            }
            .padding(8)
    }

    private var overlay: some View {
        VStack {
            topBar
            Spacer()
            if document.pageCount > 1 { bottomBar }
            else if document.pageCount == 1 { singlePageBar }
        }
    }

    private var topBar: some View {
        HStack {
            Button { store.selectedComicId = nil } label: {
                Label("Back", systemImage: "chevron.left")
            }
            Text(comic.title)
                .font(.headline)
                .lineLimit(1)
            Spacer()
            Button {
                store.toggleBookmark(comicId: comic.id, page: currentPage)
            } label: {
                Image(systemName: isBookmarked ? "bookmark.fill" : "bookmark")
                    .foregroundStyle(isBookmarked ? accent : .primary)
            }
            .help(isBookmarked ? "Remove bookmark" : "Add bookmark")

            Button { showBookmarks = true } label: {
                Image(systemName: "list.bullet.rectangle.portrait")
            }
            .help("Bookmarks")
            .popover(isPresented: $showBookmarks, arrowEdge: .bottom) { bookmarksPopover }

            Button { showTune = true } label: {
                Image(systemName: "slider.horizontal.3")
            }
            .help("Reader settings")
            .popover(isPresented: $showTune, arrowEdge: .bottom) { tunePopover }
        }
        .buttonStyle(.bordered)
        .padding()
        .background(.ultraThinMaterial)
        .overlay(alignment: .bottom) {
            Divider().opacity(0.4)
        }
    }

    private var bottomBar: some View {
        HStack(spacing: 12) {
            Button("Previous") { goToPreviousPage() }
            Slider(
                value: Binding(
                    get: { Double(currentPage) },
                    set: { currentPage = Int($0).clamped(to: 0...(document.pageCount - 1)) }
                ),
                in: 0...Double(document.pageCount - 1),
                step: 1
            )
            .tint(accent)
            Text("\(currentPage + 1) / \(document.pageCount)")
                .font(.callout.monospacedDigit())
                .foregroundStyle(.secondary)
                .frame(minWidth: 60)
            Button("Next") { goToNextPage() }
        }
        .padding()
        .background(.ultraThinMaterial)
        .overlay(alignment: .top) {
            Divider().opacity(0.4)
        }
    }

    private var singlePageBar: some View {
        HStack {
            Text("1 / 1")
            Spacer()
            Text(comic.parserMessage ?? document.parserMessage ?? "")
                .lineLimit(1)
                .foregroundStyle(.secondary)
        }
        .padding()
        .background(.ultraThinMaterial)
    }

    private var isBookmarked: Bool {
        store.bookmarks(for: comic.id).contains { $0.page == currentPage }
    }

    private var bookmarksPopover: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Label("Bookmarks", systemImage: "list.bullet.rectangle.portrait").font(.headline)
                Spacer()
                Button { showBookmarks = false } label: { Image(systemName: "xmark") }
                    .buttonStyle(.plain)
            }
            Divider()
            if store.bookmarks(for: comic.id).isEmpty {
                ContentUnavailableView("No bookmarks yet", systemImage: "bookmark",
                    description: Text("Use the bookmark button to save the current page."))
                    .frame(width: 280, height: 160)
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 8) {
                        ForEach(store.bookmarks(for: comic.id)) { bookmark in
                            Button {
                                currentPage = bookmark.page.clamped(to: 0...max(document.pageCount - 1, 0))
                                showBookmarks = false
                            } label: {
                                Label("Page \(bookmark.page + 1)", systemImage: "bookmark.fill")
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                }
                .frame(width: 300, height: 220)
            }
        }
        .padding(16)
    }

    private var tunePopover: some View {
        var local = prefs
        return VStack(alignment: .leading, spacing: 16) {
            HStack {
                Label("Reader Settings", systemImage: "slider.horizontal.3").font(.headline)
                Spacer()
                Button { showTune = false } label: { Image(systemName: "xmark") }
                    .buttonStyle(.plain)
            }
            Divider()
            VStack(alignment: .leading, spacing: 10) {
                Text("Layout").font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                Picker("Layout", selection: Binding(
                    get: { local.readerLayoutMode },
                    set: { local.readerLayoutMode = $0; store.setReaderPreferences(local) }
                )) {
                    ForEach(ReaderLayoutMode.allCases) { Text($0.label).tag($0) }
                }
                .pickerStyle(.segmented)
            }
            VStack(alignment: .leading, spacing: 10) {
                Text("Direction").font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                Picker("Direction", selection: Binding(
                    get: { local.readingDirection },
                    set: { local.readingDirection = $0; store.setReaderPreferences(local) }
                )) {
                    ForEach(ReadingDirection.allCases) { Text($0.label).tag($0) }
                }
                .pickerStyle(.segmented)
            }
            ReaderSlider(
                title: "Brightness",
                value: Binding(get: { local.brightness },
                               set: { local.brightness = $0; store.setReaderPreferences(local) }),
                range: -0.5...0.5, resetValue: 0, accent: accent
            )
            ReaderSlider(
                title: "Contrast",
                value: Binding(get: { local.contrast },
                               set: { local.contrast = $0; store.setReaderPreferences(local) }),
                range: 0.5...2, resetValue: 1, accent: accent
            )
            HStack {
                Text("Zoom").font(.callout)
                Spacer()
                Button("Reset") {
                    scale = 1
                    pageOffset = .zero
                }
            }
        }
        .padding(18)
        .frame(width: 390)
    }

    // MARK: - Actions

    private func load() {
        isLoading = true
        document = parser.loadForReader(comic)
        let maxPage = max(document.pageCount - 1, 0)
        currentPage = min(max(store.progress[comic.id]?.currentPage ?? 0, 0), maxPage)
        isLoading = false
    }

    private func goToPreviousPage() {
        let delta = prefs.readingDirection == .rtl ? 1 : -1
        currentPage = (currentPage + delta).clamped(to: 0...max(document.pageCount - 1, 0))
        resetPageTransform()
    }

    private func goToNextPage() {
        let delta = prefs.readingDirection == .rtl ? -1 : 1
        currentPage = (currentPage + delta).clamped(to: 0...max(document.pageCount - 1, 0))
        resetPageTransform()
    }

    private func handleHorizontalScroll(_ delta: CGFloat) {
        guard document.pageCount > 1, abs(delta) > 0, !didPageDuringScroll else { return }
        scrollAccumulator += delta
        if scrollAccumulator > 90 {
            didPageDuringScroll = true
            goToPreviousPage()
        } else if scrollAccumulator < -90 {
            didPageDuringScroll = true
            goToNextPage()
        }
    }

    private func resetHorizontalScrollGesture() {
        scrollAccumulator = 0
        didPageDuringScroll = false
    }



    private func resetPageTransform() {
        scale = 1
        pageOffset = .zero
        scrollAccumulator = 0
        didPageDuringScroll = false
    }
}

// MARK: - Helpers

private extension Array {
    subscript(safe index: Int) -> Element? { indices.contains(index) ? self[index] : nil }
}

private extension Int {
    func clamped(to range: ClosedRange<Int>) -> Int {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}

private extension CGFloat {
    func clamped(to range: ClosedRange<CGFloat>) -> CGFloat {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}

private extension Double {
    func formattedPercent(relativeTo baseline: Double = 1) -> String {
        "\(Int((self / baseline * 100).rounded()))%"
    }
}

// MARK: - Sub-views

private struct ReaderMessageView: View {
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 42, weight: .semibold))
                .foregroundStyle(.orange)
            Text(title).font(.title2.bold())
            Text(message)
                .font(.callout)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 520)
        }
        .padding(32)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24))
        .padding()
    }
}

private struct ReaderSlider: View {
    let title: String
    @Binding var value: Double
    let range: ClosedRange<Double>
    let resetValue: Double
    let accent: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                Spacer()
                Text(title == "Contrast" ? value.formattedPercent() : signedValue)
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(.secondary)
                Button("Reset") { value = resetValue }
                    .font(.caption)
                    .buttonStyle(.borderless)
            }
            Slider(value: $value, in: range).tint(accent)
        }
    }

    private var signedValue: String {
        let amount = Int((value * 100).rounded())
        return amount > 0 ? "+\(amount)%" : "\(amount)%"
    }
}

// MARK: - Input (Trackpad + Keyboard)

private struct ReaderInputView: NSViewRepresentable {
    var isZoomed: Bool
    var onMagnify: (CGFloat) -> Void
    var onSmartMagnify: () -> Void
    var onHorizontalScroll: (CGFloat) -> Void
    var onScrollEnded: () -> Void
    var onPan: (CGSize) -> Void
    var onPreviousPage: () -> Void
    var onNextPage: () -> Void

    func makeNSView(context: Context) -> ReaderInputNSView {
        let view = ReaderInputNSView()
        update(view)
        return view
    }

    func updateNSView(_ nsView: ReaderInputNSView, context: Context) {
        update(nsView)
    }

    private func update(_ view: ReaderInputNSView) {
        view.isZoomed = isZoomed
        view.onMagnify = onMagnify
        view.onSmartMagnify = onSmartMagnify
        view.onHorizontalScroll = onHorizontalScroll
        view.onScrollEnded = onScrollEnded
        view.onPan = onPan
        view.onPreviousPage = onPreviousPage
        view.onNextPage = onNextPage
    }
}

final class ReaderInputNSView: NSView {
    var isZoomed = false
    var onMagnify: ((CGFloat) -> Void)?
    var onSmartMagnify: (() -> Void)?
    var onHorizontalScroll: ((CGFloat) -> Void)?
    var onScrollEnded: (() -> Void)?
    var onPan: ((CGSize) -> Void)?
    var onPreviousPage: (() -> Void)?
    var onNextPage: (() -> Void)?

    override var acceptsFirstResponder: Bool { true }

    override func viewDidMoveToWindow() {
        super.viewDidMoveToWindow()
        window?.makeFirstResponder(self)
    }

    override func magnify(with event: NSEvent) {
        onMagnify?(event.magnification)
    }

    override func smartMagnify(with event: NSEvent) {
        onSmartMagnify?()
    }

    override func mouseDragged(with event: NSEvent) {
        guard isZoomed else { return }
        // deltaX positive = mouse right, deltaY positive = mouse up in AppKit (Y-up),
        // so negate deltaY to get screen-down = SwiftUI offset increases downward.
        onPan?(CGSize(width: event.deltaX, height: -event.deltaY))
    }

    override func scrollWheel(with event: NSEvent) {
        if isZoomed {
            // Pan the zoomed image. scrollingDeltaY positive = scroll up (content moves up),
            // so negate to move the image offset in the same direction as finger movement.
            onPan?(CGSize(width: event.scrollingDeltaX, height: -event.scrollingDeltaY))
            return
        }
        if abs(event.scrollingDeltaX) > abs(event.scrollingDeltaY) {
            onHorizontalScroll?(event.scrollingDeltaX)
            if event.phase == .ended || event.momentumPhase == .ended || event.phase == .cancelled {
                onScrollEnded?()
            }
        } else {
            super.scrollWheel(with: event)
        }
    }

    override func keyDown(with event: NSEvent) {
        switch event.keyCode {
        case 123, 126: onPreviousPage?() // left arrow, up arrow
        case 124, 125: onNextPage?()     // right arrow, down arrow
        case 49:       onNextPage?()     // space
        default:       super.keyDown(with: event)
        }
    }
}
