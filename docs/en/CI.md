# CI — automated build & release (GitHub Actions)

> English · [العربية](../CI.md)

Two workflows in `.github/workflows/`:

## 1. `build.yml` — build + lint
Runs on every **push/PR to `main`** (skipping docs/website-only changes).
- JDK 17 (temurin) + Android SDK (platform android-36, build-tools 35.0.0).
- `./gradlew assembleDebug lintDebug` — proves the project **builds cleanly from source**.
- Uploads the debug APK and lint report as artifacts.

## 2. `release.yml` — automated release
Runs when a **`v*` tag** is pushed (e.g. `v1.0.0`).
- Builds `assembleRelease`.
- Creates a **GitHub Release** for the tag and attaches the APK (with generated notes).
- Tags with a suffix (`v1.0.0-rc1`) are marked **prerelease**; a clean tag (`v1.0.0`) is a full release.

### Cutting a new release
```bash
git tag v1.0.0 && git push origin v1.0.0
```
The workflow builds and publishes the release. (No more manual local builds.)

## Signing in CI (optional)
Without secrets → it builds the release **unsigned**. To sign automatically with your key, add
**Secrets** in GitHub (`Settings → Secrets and variables → Actions`):

| Secret | Content |
| :-- | :-- |
| `KEYSTORE_BASE64` | the keystore file base64-encoded: `base64 -w0 my.keystore` |
| `KEYSTORE_PASSWORD` | store password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

When present, the workflow reconstructs `keystore.properties` + the key file and signs automatically
(matching `app/build.gradle.kts`). Secrets **never appear in logs** and aren't stored in git.

> F-Droid note: it doesn't use your signature; it builds from source and signs with its own key.
> CI signing is for GitHub releases only.
