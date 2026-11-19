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
| M2 | Discovery and playback: permissions, MediaStore scan, Room cache, Media3 service, notification and media controls | Delivered, awaiting CI run and on-device check |
| M3–M8 | Library, music management, player, advanced features, motion, QA and release | Not started |

Every screen other than the navigation shell is an honest placeholder that says which milestone builds it.

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
| `feature/*` | One package per screen area |

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
- **Temporary UI**: the Library tab carries an "M2 PLAYBACK CHECK" panel so real discovery and playback can be
  exercised before the Library (M3) and Player (M5) screens exist. It is removed when those are built.

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

- The Room v1 to v2 migration is hand-written and has not been run on a device or in an instrumented test.
- Shuffle order is regenerated after a restart (only the on/off state is saved).
- Notification artwork comes from MediaStore's album-art URI and may be missing for some files.
- Nothing from M2 has been compiled or run by the author; CI and a phone are the first real build.

- Space Grotesk is not bundled yet; display styles use the system sans until the font files are added
  (see `SonaraFonts`).
- Logo and launcher icon are temporary placeholders.
- No INTERNET permission, by design.
