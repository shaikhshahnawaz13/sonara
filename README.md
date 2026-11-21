# SONARA

**YOUR MUSIC. YOUR RULES.**

An offline-first native Android music player (Kotlin, Jetpack Compose, Media3) with a dark
neo-brutalist identity and acid-lime accents. SONARA V1.0 is a fresh implementation built
milestone by milestone against the SONARA V1.0 master specification.

## Status

| Milestone | Scope | State |
|---|---|---|
| M1 | Foundation: Gradle, tokens, theme, navigation skeleton, Room, DataStore, permission abstraction, CI | Delivered, CI green |
| M1.1 | Motion foundation: central tokens, push/pop navigation, travelling nav indicator, reduced motion | Delivered |
| M1.2 | Tabs as one continuous page strip; indicator driven by the strip | Delivered, verified on device |
| M2 | Discovery and playback: permissions, MediaStore scan, Room cache, Media3 service, notification and media controls | Delivered, verified on device (Bluetooth disconnect untested) |
| M3 | Library and browsing: Home, Library, Search, Folders, album/artist/genre/folder details | Delivered, awaiting CI run and on-device check |
| M4–M8 | Music management, player, advanced features, motion, QA and release | Not started |

Screens that are not built yet (Playlists, Settings) are honest placeholders that say which milestone builds them.

## Requirements

- Android 12 (API 31) or newer
- JDK 17 to build
- The Gradle wrapper is included; no global Gradle install is needed

## Build and test

```bash
./gradlew :app:testDebugUnitTest   # JVM unit tests
./gradlew :app:assembleDebug       # debug APK -> app/build/outputs/apk/debug/
./gradlew :app:lintDebug           # Android lint
```

GitHub Actions (`SONARA CI`) runs the same commands through the wrapper and uploads these artifacts:
`SONARA-debug-apk`, `SONARA-unit-test-reports`, `SONARA-lint-report`, `SONARA-room-schemas`.

## Project layout (`com.sonara.app`)

| Package | Responsibility |
|---|---|
| `app` | `MainActivity`, `SonaraApplication`, `AppContainer` (manual dependency container) |
| `core/design` | Colour, typography, shape and spacing tokens, `SonaraTheme` |
| `core/design/components` | Reusable components (top bar, bottom navigation, icon button, wordmark, placeholder) |
| `core/navigation` | `SonaraDestination` and the navigation graph |
| `core/permissions` | Version-aware audio/notification permission policy, `PermissionCoordinator` (Activity launchers), `AudioAccessModel` (process-wide state) |
| `data/local` | Room database (v2), entities, DAOs and DataStore settings |
| `data/media` | MediaStore scanner, metadata normaliser, artwork loader, scan marker |
| `data/repository` | `LibraryRepository` (scan, diff, Room cache, change observer) |
| `playback` | `PlaybackService` (ExoPlayer + MediaSession), `PlaybackController` (MediaController client), persistence and queue restore |
| `feature/*` | One package per screen area (`home`, `library`, `search`, `folders`, `detail`, plus `common` for shared ViewModel and playback-action helpers) |

## M2: discovery and playback

- **Permissions**: Android 13+ asks for `READ_MEDIA_AUDIO` (then `POST_NOTIFICATIONS`, once, after audio is granted);
  Android 12 asks for `READ_EXTERNAL_STORAGE`. Not-requested, denied and permanently-denied states each get their own
  prompt, and the state is re-read when the app returns from system Settings.
- **Discovery**: `MediaStoreScanner` reads music files; `TrackNormalizer` cleans tags; `LibraryDiffer` compares the
  result with the Room `tracks` table so only changed rows are written. A MediaStore version/generation marker lets
  launches skip the scan when nothing changed, and a content observer rescans while the app runs.
- **Playback**: one `ExoPlayer` inside one `MediaSessionService`. Audio focus, headphone/Bluetooth disconnect
  pause and the wake lock are ExoPlayer settings. The UI only holds a `MediaController` and never the player, so
  playback does not depend on any screen. Controllers send track ids; the service resolves them from Room.
- **Persistence**: queue, current track, position, shuffle and repeat are saved to Room and restored (paused) when
  the service starts; tracks deleted in the meantime are dropped.
- **Temporary UI**: M2 shipped with a playback-check panel on the Library tab; M3 removed it in favour of the real Library screens.

## M3: library and browsing

- **Home**: real counts, Continue Listening (from play history recorded by the playback service after 5 s of
  real playback), Recently Added and Quick Access. Empty, scanning, error and permission states are explicit.
- **Library**: Songs, Albums, Artists and Genres from the Room `tracks` cache. Scroll position survives changing
  category and tab. Row actions: play, play next, add to queue, view album, view artist.
- **Details**: album (track numbers, total length), artist (albums and tracks), genre and folder, each with
  Play, Shuffle and Add to queue.
- **Search**: local only, case-insensitive, every word must match, ranked title-prefix first, debounced and run
  off the main thread; recent searches are kept in DataStore.
- **Folders**: folders derived from indexed tracks, plus folders granted through the system picker (persisted
  read grants, status re-checked on resume). A granted folder lists the indexed tracks inside it.
- **Basic mini player**: a stand-in above the bottom navigation (artwork, title, play/pause, next). M5 replaces
  it with the real mini-player and Full Player.
- Not shown on purpose because they belong to M4: Add to playlist, Favourite, playlist creation and detail.

## Motion

All motion numbers live in `core/animation/SonaraMotion.kt` (`MotionTokens`). Components ask
`SonaraTheme.motion` for specs and never define their own timing. Rules:

- No fades, crossfades or alpha-based screen transitions anywhere.
- The four tabs are one continuous horizontal strip of pages (`core/navigation/SonaraTabs.kt`). Moving
  between tabs scrolls the strip, so every page in between passes through the viewport. Tapping again
  while it moves redirects it from where it is, carrying its speed.
- Deeper screens (Search, Settings, later details) slide in from the right over the old screen, which
  drifts back by 25%; back reverses it.
- The lime navigation indicator is a single element positioned from the strip's scroll offset, so it
  moves exactly with the pages.
- Reduced motion = shorter durations and no parallax. Off = everything snaps. The in-app setting and the
  system "remove animations" scale are combined in `app/MotionPlumbing.kt`.

## Known limitations

- Room schema is still version 2; M3 added no tables.
- A folder granted through the system picker only lists audio the MediaStore scan has indexed; unindexed files in it are not playable yet.
- Artists are grouped by the track artist tag; multi-artist tags are not split.
- Search matches names only (no lyrics, no genre), and playlists cannot be created until M4.
- No shared-element artwork transition between album card and detail (planned with M7 motion).
- Shuffle order is regenerated after a restart (only the on/off state is saved).
- Notification artwork comes from MediaStore's album-art URI and may be missing for some files.
- Nothing from M3 has been compiled or run by the author; CI and a phone are the first real build.

- Space Grotesk is not bundled yet; display styles use the system sans until the font files are added
  (see `SonaraFonts`).
- Logo and launcher icon are temporary placeholders.
- No INTERNET permission, by design.


## M3.1 refinement

- **Library category transition**: Songs / Albums / Artists / Genres slide horizontally (300 ms, the bottom-navigation curve, no overshoot) in the direction of travel; the lime indicator travels with them. Each category keeps its own scroll state. Reduced motion shortens it, "off" snaps.
- **Scrolling**: artwork is served from memory synchronously when cached, decoded off the main thread at most 3 at a time, and abandoned when its row scrolls away; album tiles no longer use `BoxWithConstraints`; rows no longer observe the whole playback state; lazy items carry stable keys and content types.
- **Logo and icons**: `branding/sonara-logo-source.webp` is the supplied logo. The launcher icon, splash icon and in-app mark are traced from it (not redrawn). `SonaraIcons` is the single icon family.
- **Judging smoothness**: use the `perf` build (`./gradlew :app:assemblePerf`, uploaded by CI as `SONARA-perf-apk`). Debug builds run Compose unoptimised and understate smoothness.


## M4 - Playlists

- Create (with a custom name), rename and delete playlists; names are trimmed, limited to 60 characters and unique ignoring case.
- The Playlists tab lists every playlist with its real track count, total duration and the artwork of its first available track. Tapping opens the playlist.
- Add songs from any song menu (Home, Library, Search, album, artist, genre, folder and playlist screens) and whole albums, artists, genres and folders through ADD TO PLAYLIST. Tracks already in the playlist are skipped and reported.
- In a playlist: play from any song, PLAY, SHUFFLE, ADD TO QUEUE, remove a song, reorder by dragging the grip (or MOVE UP / MOVE DOWN in the row menu).
- Entries whose audio file is no longer in the library stay visible as "Unavailable track" and can be removed one by one or all at once; they are skipped when playing.
- Playlists live in the existing Room tables `playlists` and `playlist_tracks` (no schema change, no migration), so they survive restarts.

## M4.1 - Playlist UX refinement

- **Overflow menu**: now a popup positioned from the tapped button's real window bounds. It covers the button, flips above it when there is no room below, and stays inside the screen and system bars. It grows out of the button's outline (260 ms) and folds back into it (200 ms); items appear in a short sequence. Back, tapping outside or choosing an action closes it; only one menu is open at a time. Reduced motion shortens it, "off" snaps.
- **BROWSE SONGS / ADD SONGS**: opens a multi-select song picker (route `playlist/{id}/add`) with artwork, title, artist and album, a live "N SELECTED" count, SELECT ALL, DESELECT ALL and ADD TO PLAYLIST. Songs already in the playlist are shown as IN PLAYLIST and cannot be selected, so nothing is duplicated.
- **EDIT mode**: EDIT in the playlist's top bar. Rows get checkboxes; the bottom area offers SELECT ALL, CLEAR SELECTION, REMOVE SELECTED (with confirmation) and ADD SONGS. Dragging, MOVE UP / DOWN and the per-song menu still work. DONE or Back leaves without changing anything. Removing only deletes playlist entries, never audio files.
- No schema change.

## M5 - Player experience

- **Mini-player**: artwork, title, artist, play/pause (shows "pause" as soon as play was pressed, accent while buffering) and next. Tapping the artwork/text area opens the Full Player.
- **Full Player**: one surface that grows from the mini-player's real bounds to the full screen (420 ms open, 340 ms close, driven by a single progress value). Artwork, title, artist, play and next travel to their full-player places; collapse, queue, favourite, album, previous, shuffle, repeat, seek bar and times are revealed in sequence. Back or the chevron collapses it; it is saved across rotation. Seek bar: tap or drag, the seek is sent when the finger lifts. Shuffle and repeat (off / all / one) drive the real player. Favourite uses the existing `favourites` table.
- **Queue**: a panel over the player listing the real queue; tap to play an entry, cross to remove it.
- **Overflow menu**: its animation now starts only once the popup window is on screen.
- **Playlist EDIT mode**: EDIT/DONE and the title slide into each other, the checkbox column opens in the layout, the bottom bar expands vertically; scroll position is untouched.
- No schema change; no second player instance.

## M5.1 - Focused polish

- **Favourite heart**: now adds to and removes from the real "Favourites" playlist through `PlaylistRepository.toggleFavourite`
  (one Room transaction; creates the playlist if missing; no duplicates). The icon reads the playlist's track ids, so it
  follows track changes and survives restarts. A failed write shows a snackbar and leaves the icon unchanged.
  The old standalone `favourites` table is no longer used.
- **Playlist EDIT transition**: same animation; row placement is snapped while it runs and the checkbox column is
  revealed at draw time only, so rows are not re-measured per frame. Inferred from code, not profiled: verify on a device.
- **Repeat One**: the "1" is centred on the loop and drawn with a bevelled join.
