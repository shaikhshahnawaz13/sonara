# SONARA v0.5.0

## Focus: navigation only

- Rebuilt the bottom navigation to follow the supplied floating-glass navigation reference.
- Preserved the three SONARA destinations: Home, Search, and Library.
- Inactive destinations use icon-only presentation on a pure black rounded rail.
- The active destination sits in a raised, rounded refractive capsule with upper/lower reflective lips, cool edge refraction, a white icon, and its label.
- Kept the existing navigation destination handling and page rendering logic.
- Did not intentionally alter splash timing, page transitions, playback, content screens, or other UI surfaces.

This is a navigation-focused version. Applying the same material treatment to the rest of the UI is deliberately deferred to a later version.

## Verification

Source changes were packaged. A local Android build could not be run in this environment because Gradle and the Android SDK are not installed. Use the repository's GitHub Actions workflow to verify compilation before installing the APK.
