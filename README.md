# Panelix

Panelix is a native, local-first comic reader project with Android and macOS apps maintained side by side.

## Apps

| App | Path | Stack | Repository |
| --- | --- | --- | --- |
| Android | [`Android/`](Android/) | Kotlin, Jetpack Compose, Room, Material 3 | `https://github.com/amiralibg/Panelix.git` |
| macOS | [`macOS/`](macOS/) | Swift, SwiftUI, Xcode | `https://github.com/amiralibg/Panelix-Macos.git` |

## Repository Strategy

This folder can be hosted in two common ways:

1. **Monorepo**: one Git repository owns both app folders. This is the simplest setup if you want one GitHub repository for the full Panelix project.
2. **Multi-repo with submodules**: the root repository tracks `Android/` and `macOS/` as separate Git repositories. This is best if you want each app to keep its own remote, issue tracker, releases, and commit history.

Because `Android/` and `macOS/` already have their own `.git` folders and remotes, the safest multi-repo setup is Git submodules.

## Recommended Multi-Repo Setup

From the parent folder of this project, first make sure each app has no uncommitted work that you care about:

```bash
git -C Panelix/Android status
git -C Panelix/macOS status
```

Commit or stash app changes before continuing. Then create a root repository and add the existing app repositories as submodules:

```bash
cd Panelix

# Create the root repository.
git init

# Temporarily move the current app folders out of the way.
cd ..
mv Panelix/Android Panelix-Android-existing
mv Panelix/macOS Panelix-macOS-existing
cd Panelix

# Add the existing remotes back as submodules.
git submodule add https://github.com/amiralibg/Panelix.git Android
git submodule add https://github.com/amiralibg/Panelix-Macos.git macOS

# Keep root-level documentation in the parent repository.
git add .gitmodules README.md LICENSE
git commit -m "Create Panelix multi-repo workspace"
```

After confirming the cloned submodules contain everything you need, you can remove the temporary folders:

```bash
cd ..
rm -rf Panelix-Android-existing Panelix-macOS-existing
```

To clone the root repository later with both apps:

```bash
git clone --recurse-submodules <root-repo-url>
```

If it was cloned without submodules:

```bash
git submodule update --init --recursive
```

## Working With Submodules

Update all apps to their recorded commits:

```bash
git submodule update --init --recursive
```

Pull the latest code inside each app:

```bash
git -C Android pull
git -C macOS pull
```

After updating an app, commit the new submodule pointer in the root repository:

```bash
git status
git add Android macOS
git commit -m "Update app submodule revisions"
```

## Build

Android:

```bash
cd Android
./gradlew :app:assembleDebug
```

macOS:

```bash
cd macOS
open PanelixDesktop.xcodeproj
```

## License

Panelix is open source under the MIT License. See [LICENSE](LICENSE).
