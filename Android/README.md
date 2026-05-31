<p align="center">
  <img src="docs/panelix-icon.svg" width="120" height="120" alt="Panelix icon">
</p>

<h1 align="center">Panelix</h1>

<p align="center">
  A native, local-first Android comic reader built with Kotlin and Jetpack Compose.
</p>

## Overview

Panelix is a local comic library and reader for Android. It uses the Android Storage Access Framework, so users choose comic folders explicitly and the app keeps everything local on the device. There are no accounts, cloud sync, remote library services, or JavaScript runtime.

The app is currently version `0.0.1`.

## Features

- Local-first comic library backed by Room/SQLite
- Android folder picking with persisted SAF permissions
- Recursive folder scanning
- Comic detection for:
  - PDF
  - CBZ
  - CBR
  - image folders with at least 3 image files
  - CBT and CB7 detection with unsupported-state messaging
- Native reader with:
  - horizontal paging by default
  - vertical scrolling
  - spread mode for larger/landscape layouts
  - left-to-right and right-to-left reading direction
  - pinch-to-zoom, pan, and double-tap zoom
  - page scrubber and jump-to-page behavior
  - bookmarks and bookmark sheet
  - brightness and contrast controls
  - automatic reading progress
  - immersive full-screen mode
- Library tools:
  - search
  - sort by recently added, recently opened, or title
  - grid/list view
  - format filters
  - continue reading section
  - scan progress with estimated remaining time
- Light, dark, and system theme preferences

## Architecture

Panelix is a native Android app. It does not use React Native, Expo, WebView, or JavaScript.

Main technologies:

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Room
- DataStore
- Kotlin Coroutines and Flow
- Android Storage Access Framework
- Android `PdfRenderer`
- Native file/cache APIs
- Coil for image display
- 7-Zip-JBinding for CBR/RAR extraction

The parser layer is isolated behind a `ComicParser` interface so PDF, CBZ, CBR, and image-folder support can evolve independently.

## Storage And Privacy

Panelix stores all app data locally:

- selected folders
- comic metadata
- covers
- extracted page cache
- reading progress
- bookmarks
- reader preferences
- app preferences

The app does not upload comics, create accounts, or sync data to a server.

## Supported Formats

| Format | Status | Notes |
| --- | --- | --- |
| PDF | Supported | Rendered with Android `PdfRenderer` |
| CBZ | Supported | Uses streaming Zip APIs |
| CBR | Supported | Uses native 7-Zip-JBinding extraction |
| Image folder | Supported | Folder must contain at least 3 image files |
| CBT | Detected | Marked unsupported for now |
| CB7 | Detected | Marked unsupported for now |

## Build Requirements

- Android Studio
- JDK 17
- Android SDK with the configured compile SDK
- Gradle wrapper included in the repository

## Build

Debug APK:

```bash
./gradlew :app:assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Run tests:

```bash
./gradlew :app:testDebugUnitTest
```

Install debug build on a connected device or emulator:

```bash
./gradlew :app:installDebug
```

Release APK:

```bash
./gradlew :app:assembleRelease
```

Unsigned release APK output:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

For a signed release APK, use Android Studio:

1. Open the project.
2. Select `Build` -> `Generate Signed App Bundle / APK`.
3. Choose `APK`.
4. Create or select a keystore.
5. Select the `release` build variant.
6. Generate the signed APK.

## Versioning

The app version is configured in:

```text
app/build.gradle.kts
```

Current version:

```kotlin
versionCode = 1
versionName = "0.0.1"
```

Increase `versionCode` for every distributed release. Change `versionName` for the user-visible version.

## Project Structure

```text
app/src/main/java/com/amiralibg/panelix/
  cache/        Cache and extracted page storage
  data/         Room entities, DAOs, database, preferences
  di/           Manual dependency graph
  parser/       Comic parser interfaces and implementations
  repository/   App repository layer
  scanner/      Folder scanner and comic detection
  ui/           Compose screens, navigation, theme, components
```

## License

Panelix is open source under the MIT License. See [LICENSE](LICENSE).
