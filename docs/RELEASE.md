# SONARA release signing

The signing key is the identity of the app. **Never commit it, never put it in a patch ZIP, never paste its password
in chat.** If it is lost, installed copies can no longer be updated; if it leaks, anyone can sign as SONARA. Keep an
offline backup (a password manager attachment or an encrypted copy on a second device).

## 1. Create the keystore once (Termux)

```bash
pkg install openjdk-17
keytool -genkeypair -v -keystore ~/sonara-release.jks -alias sonara \
  -keyalg RSA -keysize 4096 -validity 10000
```
Choose a strong password when prompted (the key password may be the same as the store password). Back the file up
somewhere safe, outside the repository folder.

## 2. Give GitHub the secrets (not the repository)

```bash
base64 -w0 ~/sonara-release.jks > ~/sonara-release.b64
```
In the repository: Settings > Secrets and variables > Actions > New repository secret. Create:

| Secret | Value |
|---|---|
| `SONARA_KEYSTORE_BASE64` | contents of `sonara-release.b64` |
| `SONARA_KEYSTORE_PASSWORD` | the store password |
| `SONARA_KEY_ALIAS` | `sonara` |
| `SONARA_KEY_PASSWORD` | the key password |

Delete `sonara-release.b64` afterwards.

## 3. Build

Actions > "SONARA Release (signed)" > Run workflow. It runs the unit tests, builds the signed APK and AAB, verifies
the APK signature with `apksigner`, deletes the keystore from the runner and uploads `SONARA-release-apk`,
`SONARA-release-aab` and `SONARA-release-mapping`. The workflow stops with a clear error if a secret is missing.

## Local builds

Copy `keystore.properties.example` to `keystore.properties` (it is git-ignored), fill it in and run
`./gradlew :app:assembleRelease`. Without it, `assembleRelease` still works but produces an unsigned APK that Android
will not install.
