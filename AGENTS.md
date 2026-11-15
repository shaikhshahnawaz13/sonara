# SONORA engineering rules

## Product principles
- Native Android application using Kotlin and Jetpack Compose.
- Premium, restrained dark visual language. Glassmorphism is a material accent, not a blur applied to every surface.
- Offline-first. Local music must remain usable without network access.
- No ads, tracking SDKs, paid APIs, subscription dependencies, or deceptive download controls.
- Provider terms and per-track licences must be checked before streaming, caching, or downloading.

## Engineering rules
- Do not claim a feature is complete unless it works end-to-end.
- Never use mock catalogue data as real search results.
- Keep UI, domain, data, and playback responsibilities separated.
- Avoid blocking the main thread.
- Use stable list keys and immutable UI state.
- Keep animations short, purposeful, interruptible, and mindful of reduced-motion preferences.
- Handle loading, empty, permission-denied, offline, and error states.
- Add tests for business logic and important UI flows.
- Do not commit secrets, signing keys, or private credentials.
- Test on multiple screen sizes and Android versions before release.
