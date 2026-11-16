# SONARA v0.5.0

Native Android music app source.

## This version

This release focuses only on the bottom navigation, matching the supplied floating refractive-glass reference as closely as possible while retaining SONARA's Home, Search, and Library destinations. Other screens and their existing visual styling are intentionally unchanged in this version.

## Build

```bash
gradle :app:assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

The GitHub Actions workflow also builds and uploads the debug APK as an artifact.

## Verification note

The source archive was prepared, but a local Android build was not run in the packaging environment. Confirm the build through GitHub Actions before installing.
