# SONARA

An offline, native Android music player (Kotlin, classic Views, no third-party runtime dependencies) with a dark/light Neo-morphic interface.

## Structure
- `MainActivity.kt` window, insets, navigation, permissions
- `Theme.kt`, `NeoDrawable.kt`, `Ui.kt` design tokens and reusable components
- `Screens.kt`, `Shell.kt`, `PlayerScreen.kt` screens, navigation bar, mini-player, player
- `Data.kt` MediaStore library, liked songs, playlists
- `Playback.kt` foreground playback service and shared playback state

## Build
GitHub Actions (`Android CI`) builds `app-debug.apk` and uploads it as the `sonora-debug` artifact. On failure it uploads `build-log`.
