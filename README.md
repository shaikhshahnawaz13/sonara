# SONARA

**YOUR MUSIC. YOUR RULES.**

An offline-first native Android music player (Kotlin, Jetpack Compose, Media3) with a dark
neo-brutalist identity and acid-lime accents. SONARA V1.0 is a fresh implementation built
milestone by milestone against the SONARA V1.0 master specification.

## Status

| Milestone | Scope | State |
|---|---|---|
| M1 | Foundation: Gradle, tokens, theme, navigation skeleton, Room, DataStore, permission abstraction, CI | Delivered, awaiting first CI run |
| M2 | Discovery and playback | Not started |
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
| `core/permissions` | Version-aware audio permission policy and checker |
| `data/local` | Room database, entities, DAOs and DataStore settings |
| `feature/*` | One package per screen area |

Packages for playback, media discovery and repositories are created in the milestone that needs them.

## Known M1 limitations

- Space Grotesk is not bundled yet; display styles use the system sans until the font files are added
  (see `SonaraFonts`).
- Logo and launcher icon are temporary placeholders.
- No INTERNET permission, by design.
