# SONORA

SONORA is an offline-first native Android music player built with Kotlin and Jetpack Compose.

## Current foundation milestone
- Premium dark visual foundation.
- Home, Search, and Library navigation.
- Local device audio discovery after explicit user action.
- Track list and metadata presentation.
- Runtime audio permission request.
- Media3 session service and controller bridge for local track playback.

## Build
Open this project in a recent stable Android Studio release and allow Gradle sync.
Then run the `app` configuration on an Android device or emulator.

Requirements:
- JDK 17
- Android SDK 36
- Android Gradle Plugin 8.13.2
- Gradle 8.13 or compatible wrapper supplied by Android Studio

## Important status
This is the first source milestone, not a finished release. The next implementation tasks are:
1. Add robust player reconnection, queue state, seek controls, and playback restoration.
2. Add Room persistence for playlists, favourites, and history.
3. Add provider integrations only after verifying their current licences and API terms.
4. Add automated tests and CI build verification on a real Android toolchain.

## Privacy
Local music discovery is performed on-device. No audio files are uploaded.
