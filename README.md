# SONORA Android

SONORA is a native Android offline music player. This milestone is **v0.3.0** and focuses on a refreshed cyan/night interface while retaining local audio scanning and playback.

## Current features
- Scans audio indexed by Android MediaStore
- Lists local tracks and filters them in Search
- Plays and pauses local audio
- Home, Search and Library navigation
- Cyan/night visual system, translucent layered cards and restrained transitions
- Official SONORA SVG logo retained in `app/src/main/assets/sonora_logo.svg` and rasterized for the Android UI

## Build
Open this project in Android Studio and run `:app:assembleDebug`, or push to GitHub to use the Android CI workflow. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

This is not v1.0 yet. Streaming catalogues, richer playback queue, artwork, background playback controls, playlists and full device testing remain future work.
