# Device & code database — local/offline or internet-dependent?

> English · [العربية](../DATABASE.md)

**Guiding principle: offline by default.** All core features work without internet.
Internet is optional only to expand IR codes for rare brands, and anything downloaded is saved locally so it isn't requested again.

The design is two fully separate layers:

## 1. Network device discovery (m1) — no database, 100% offline

Smart TVs, Android boxes (original & Chinese) and IP devices **advertise themselves** on the local
WiFi network. The app picks up the advertisement (name, type, sometimes model) and infers the
protocol via `ServiceFingerprint` — **without internet and without any stored database**. The device itself is the "database".

## 2. IR codes (m4) — here a database appears, with a local-first hybrid design

| Layer | Source | Internet? | Status |
| :-- | :-- | :-- | :-- |
| **Bundled core** | `assets/irdb/` — 67 devices (TV 35 + Cable 25 + Audio 7) from IRRemote + NEC/RC5/RC6 import from probonopd/irdb, in a unified format (index.json + per-device file, Pronto + frequency). | ❌ fully offline | ✅ done |
| **Extension** | Online search of probonopd/irdb: a 3244-codeset index ships as an asset (offline search) + the chosen codeset is downloaded via CDN and converted to Pronto on the phone (`IrCodeConverter`) and saved locally. | ✅ fetch only | ✅ done (0.9.9) |
| **User packs** | Shared `.tahakom` files (see [SHARING.md](../SHARING.md)). | ❌ offline | done |
| **Learn** | Capture the signal from an original remote via the phone's receiver (if present). | ❌ offline | planned |

### Why local-first?
- Home use: the phone may be on a local WiFi with no working internet.
- Privacy & speed: no reliance on an external server for core features.
- Robustness: the app doesn't "break" if internet is absent or the LIRC database closes later.

### Technical storage
- **Room/DataStore**: saved devices + custom remotes + user codes + cached downloads.
- **assets/**: the bundled read-only database (loaded on first brand request).
- **DataStore**: preferences (language, last network, "allow online download" option).

## Summary

| Function | Needs internet? |
| :-- | :-- |
| Network device discovery (m1) | ❌ No |
| Network control (m2–m3) | ❌ No (local network) |
| IR codes for popular brands (m4) | ❌ No (bundled) |
| IR codes for a rare, non-bundled brand | ✅ Once only (then saved) |
| Import/share `.tahakom` packs | ❌ No |
