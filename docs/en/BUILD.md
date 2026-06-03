# Building, packaging & testing

> English · [العربية](../BUILD.md)

## Quick build (development)

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk (~18 MB, un-minified)
./gradlew installDebug       # install directly on a connected device/emulator
```

## Signed release build (testing/distribution)

The signed build is minified by R8 and strips unused resources (~1.6 MB).

```bash
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk
```

### Signing
Read from `keystore.properties` at the project root (**not committed to git**):

```properties
storeFile=gt-tahakom-release.jks
storePassword=********
keyAlias=gt-tahakom
keyPassword=********
```

If the file is absent, the project builds the release **unsigned** (for environments without the key).
To create a new keystore:

```bash
keytool -genkeypair -v -keystore gt-tahakom-release.jks \
  -alias gt-tahakom -keyalg RSA -keysize 2048 -validity 10000
```

> ⚠️ Keep `gt-tahakom-release.jks` and `keystore.properties` somewhere safe outside git.
> Losing the key means you can't update the app under the same signature later.

## Installing for testing

Ready APKs live in `release/GT-TAHAKOM-<version>-<milestone>-release.apk`.

**Over USB:**
```bash
adb install -r release/GT-TAHAKOM-<version>-*.apk   # e.g. GT-TAHAKOM-0.9.19-polish.apk
```

**Manually:** copy the APK to the phone and open it (enable "install from unknown sources").
Ready builds are also published on [GitHub Releases](https://github.com/SalehGNUTUX/GT-TAHAKOM/releases).

## CI

Building/releasing is automated via GitHub Actions — see [CI.md](CI.md). Tag `v*` to cut a release.

## What you can test

- **Discovery:** "Find devices" button → live scan (mDNS+SSDP) finds smart TVs and Roku/Cast.
- **Network control:** LG webOS and Samsung Tizen over the network (first connection prompts acceptance on the TV).
- **IR control:** "Add by name/model" → pick a brand → semi-automatic setup → a working remote
  (requires an IR emitter in the phone, or a Broadlink bridge — experimental).
- **Manual learning:** enter Pronto codes for an unlisted device (see [LEARN_CODES_GUIDE.md](../LEARN_CODES_GUIDE.md)).
- **Settings:** language switch (Arabic/English) + theme (light/dark/system).
- **Sharing:** open a `.tahakom` file → import screen.

> The app works **offline** for core features — just WiFi on the same network as the devices.
