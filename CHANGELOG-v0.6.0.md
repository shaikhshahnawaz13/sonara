# SONARA v0.6.0 — Neo-morphism rebuild

## Fixed
- Build error `Unresolved reference: singleLine` (now `setSingleLine(true)`).
- Tapping play while a track was still preparing crashed the app (`IllegalStateException`).
- Rotating the phone or recreating the activity stopped playback and reset the UI. Playback now lives in a foreground service; the activity is portrait-locked and survives configuration changes.
- Player play/pause icon showed the wrong state on first open.
- Greeting was always "Good evening"; settings showed a stale 0.4.0 version.
- Content could sit under the status bar / gesture bar on Android 15 (edge-to-edge is now handled with window insets, including the keyboard).

## New
- Complete Neo-morphic design system (dark and light) with three elevation levels, raised / inset / pressed states, cached rendering.
- Reusable components: raised buttons, inset search field, switch, seek bar, track rows, album tiles, bottom navigation with a gliding inset indicator, mini-player with progress.
- Foreground playback service with MediaSession, notification controls, lock-screen metadata and artwork, audio focus, headphone-unplug pause, Bluetooth/headset buttons.
- Working seek bar, elapsed/total time, shuffle, repeat (off/all/one), sleep timer, system equalizer launcher.
- Real Liked songs and Playlists (stored on device), real Albums and Artists grouping, album artwork.
- Library refreshes automatically when media changes; scan runs off the main thread.
- New launcher icon (adaptive), splash icon, and in-app logo.
- Screen transitions and player slide-up (respect the system "remove animations" setting).
- CI uploads a build log and prints compiler errors when a build fails.

## Removed (were placeholders, not real features)
- Podcasts / Radio chips, "Made for You" mixes, mood tiles, genre chips, fake Downloads/Settings rows.

## Not done yet
- Queue editing, lyrics, crossfade/gapless, play history, Room database, automated tests.
- Playback uses `MediaPlayer` (no new runtime dependencies); moving to Media3 is a later option.
