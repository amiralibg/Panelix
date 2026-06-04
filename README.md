<p align="center">
  <img src="Android/docs/panelix-icon.svg" width="120" height="120" alt="Panelix icon">
</p>

<h1 align="center">Panelix</h1>

<p align="center">
  A native, local-first comic reader project for Android and macOS.
</p>

## Overview

Panelix is organized as a monorepo: one Git repository at the project root contains all platform apps and shared project documentation.

The Android app is the mature implementation today, and the macOS folder now contains a native SwiftUI desktop implementation that mirrors the Android app capabilities where macOS-native APIs are available.

## Apps

| App | Path | Status | Stack |
| --- | --- | --- | --- |
| Android | [`Android/`](Android/) | Active | Kotlin, Jetpack Compose, Room, Material 3 |
| macOS | [`macOS/`](macOS/) | Native desktop implementation started | Swift, SwiftUI, PDFKit, Xcode |

## Monorepo Structure

```text
Panelix/
  README.md
  LICENSE
  .gitignore
  Android/      Active Android app
  macOS/        Native macOS app
```

This repository intentionally uses one root `.git` folder instead of separate Git repositories inside each app folder. That keeps versioning, issues, documentation, and releases easier to manage while Panelix is developed as one product across platforms.

## Android App

The Android app is a native comic reader that keeps the user's library local on-device. It uses the Android Storage Access Framework, so users choose comic folders explicitly and the app keeps persisted access to those folders.

Current Android features include:

- Local-first comic library backed by Room/SQLite
- Folder picking with persisted Android SAF permissions
- Recursive folder scanning
- Comic detection for PDF, CBZ, CBR, image folders, CBT, and CB7
- Native reader with paging, scrolling, spread mode, reading direction, zoom, bookmarks, and page navigation
- Library search, sorting, grid/list view, filters, and continue-reading support
- Light, dark, and system theme preferences

## Android Build

Requirements:

- Android Studio
- JDK 17
- Android SDK with the configured compile SDK
- Gradle wrapper included in [`Android/`](Android/)

Build debug APK:

```bash
cd Android
./gradlew :app:assembleDebug
```

Run unit tests:

```bash
cd Android
./gradlew :app:testDebugUnitTest
```

Install debug build on a connected device or emulator:

```bash
cd Android
./gradlew :app:installDebug
```

## macOS App

The macOS app in [`macOS/`](macOS/) is now a native SwiftUI desktop implementation. It supports local folder selection with security-scoped bookmarks, recursive library scanning, search/sort/filter controls, grid/list views, reading progress, bookmarks, reader preferences, PDF reading through PDFKit, CBZ/ZIP extraction through macOS archive services, CBR/RAR extraction through local tools such as `7zz` or `unar`, and image-folder reading. CBT/TAR and CB7/7z are detected and shown with unsupported-state messaging until native extractor integrations are added.

Build the macOS app:

```bash
xcodebuild -project macOS/PanelixDesktop.xcodeproj -scheme PanelixDesktop -configuration Debug -derivedDataPath /tmp/PanelixDerivedData build
```

## Git Notes

This is now a monorepo. Use normal Git commands from the root folder:

```bash
git status
git add .
git commit -m "Describe your change"
git push
```

The repository ignores macOS `.DS_Store` files through [`.gitignore`](.gitignore).

## License

Panelix is open source under the MIT License. See [LICENSE](LICENSE).
