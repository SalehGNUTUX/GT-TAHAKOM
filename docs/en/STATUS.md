# Project status — GT-TAHAKOM

> English · [العربية](../STATUS.md)

> Last updated: 2026-06-04 · Current version: **1.0.0** (versionCode 30) · License: GPLv3
> Website & live demo: https://salehgnutux.github.io/GT-TAHAKOM/
> Repo: https://github.com/SalehGNUTUX/GT-TAHAKOM

A comprehensive reference of what's done and what's left. Updated each milestone.

---

## Overview

GT-TAHAKOM is an Android app (Kotlin + Jetpack Compose) for controlling TVs and electronics over
multiple paths, working **fully offline** for core features. Arabic/English with RTL. The
architectural core is an **abstract `Transport` layer** that picks the right path for each device.

---

## Done ✅

### Foundation (m0)
- Kotlin + Compose + Material 3, Hilt, Room/DataStore, OkHttp/Coroutines.
- Abstract `Transport` layer + `TransportRegistry` + `TransportResult`.
- Unified model: `Device` / `Command` / `ButtonId` / `Remote`.
- Release signing + APK build ([BUILD.md](BUILD.md)).

### Automatic network discovery (m1) — offline
- `MdnsDiscovery` (NsdManager): Android TV/Box, Cast, Roku, AirPlay.
- `SsdpDiscovery` (UDP multicast): Samsung/LG/Sony over UPnP, with multiple targeted queries.
- `DiscoveryManager` (merge + MulticastLock) + `ServiceFingerprint` (protocol/brand inference).
- A live devices/scan screen with an animated radar.

### Real network control (m2 + m3 partial)
- `RokuTransport` (ECP/HTTP) — real network sending, no pairing.
- `WebosTransport` (LG webOS / SSAP WebSocket :3000) — **does what IR can't**: launch apps/smart
  settings, media, volume/channel via explicit `ssap://` commands. Pairs with a saved client-key
  (`WebosKeyStore`). Navigation (arrows/OK/back) via the pointer input socket — complete.
- `SamsungTizenTransport` (Samsung Tizen / WebSocket wss :8002) — KEY_* keys, token saved after
  user acceptance, lenient local TLS.
- Full settings: light/dark/system theme (DataStore) + language + transport info + about.
- Terminology: "جهاز تحكّم" instead of "ريموت" in all UI text.
- `RemoteScreen` — a generic remote that shows each protocol's supported buttons only.

### IR layer + local database (m4 partial) — offline
- `assets/irdb/`: **67 devices** (TV 35 + Cable 25 + Audio 7) from IRRemote + NEC/RC5/RC6 import
  from probonopd/irdb, in a unified format. `tools/convert_ir_db.py` + `tools/import_irdb.py`.
- **Semi-automatic IR setup** with next/previous to step through category candidates.
- `IrDatabase`, `Pronto` (Pronto→freq+pattern), `IrCommandResolver`, `IrTransport` (ConsumerIrManager).

### Online device search (the "extension" layer)
- Offline-first hybrid: the full probonopd index (3244 codesets, **2584 supported**) ships as an
  asset, so brand search is offline (with a device-type filter: all/TV/receiver/audio). The chosen
  codeset is fetched via CDN and **converted to Pronto on the phone** (`IrCodeConverter`:
  NEC/RC5/RC6/Sony SIRC/Panasonic/JVC/Mitsubishi/Denon/Pioneer/Proton) and saved locally.

### Features
- **Manual learning** for undocumented devices (Unionaire…) by entering Pronto codes.
- **Touchpad** for network devices (swipe to navigate, tap to select).
- **Language switch** Arabic/English (per-app locale).
- **`.tahakom` sharing** (brand/model) + direct open (incl. external file-open import).
- **"My devices"** list (DataStore): open/share/delete/reorder.
- **Onboarding** (3 slides) on first launch.

### Design
- All screens on the serene token system (OKLCH→sRGB, light/dark, transport colors).
- Material outlined icons via `TahakomIcons`.

### CI — GitHub Actions
- `build.yml`: debug build + lint on every push/PR (proves a clean source build).
- `release.yml`: on a `v*` tag, builds release, creates the release and attaches the APK
  (optional automatic signing via Secrets). ([CI.md](CI.md)).

### Polish & distribution (m5) — ✅ done
- **First stable release `v1.0.0`** (versionCode 30) published via CI with the signed build attached.
- **Full CI** (build+lint on every push, automatic release on tag) — above.
- **Final bilingual docs**: every `docs/` file has an `docs/en/` counterpart; cross-linked
  Arabic/English READMEs with a dynamic version badge.
- **Website & live demo**: a self-contained landing page (Settings-style brand + a demo that
  mirrors the real remote screen with Material effects) + direct-download / releases buttons. ([WEBSITE.md](WEBSITE.md)).
- **F-Droid prep**: build recipe `fdroid/com.gnutux.tahakom.yml` + fastlane metadata
  (title/description/changelogs ar+en + icon + 4 screenshots) + submission guide
  ([FDROID.md](FDROID.md)). One action remains for the owner: open the RFP/MR on GitLab.

---

## Remaining ⏳

- **`AndroidTvTransport`** (Remote v2 + TLS pairing) — **added as an experimental "under development"
  scaffold** (v0.9.16): hand-written minimal protobuf + TLS client cert + 6-digit polo pairing +
  RemoteKeyInject. **Needs testing/tuning on real hardware** — shown to the user tagged "experimental".
  📋 First field test (SENIC device) logged in [TEST_NOTES.md](TEST_NOTES.md#1).
- **`BroadlinkTransport`** (WiFi-IR bridge) — **experimental scaffold** (v0.9.18): discovery + AES
  auth + Pronto→Broadlink packet + UDP send. Untested on real hardware.
- **More converter protocols**: 2584/3244 supported (10 families). Largest remaining: Aiwa, Dish,
  RECS80, MCE, RCA (needs MSB bit order) — added carefully after verifying a reference.
- **App shortcuts** (Netflix/YouTube) — rely on network protocols.
- **F-Droid publishing** — build recipe + fastlane metadata ready ([FDROID.md](FDROID.md)); awaiting the owner's RFP/MR submission on GitLab.
- Low priority: Glance widget, drag-reorder devices, custom Arabic fonts, visual editor.

---

## Critical technical notes (lessons)
- **AppCompatActivity requires a `Theme.AppCompat` theme** or it crashes instantly (needed for the language switch).
- **Don't hand-write an SVG path parser** for ImageVector — glued arc flags crash at runtime. Use Material Icons.
- Local platform: android-36 (not 35) → compile/target=36, AGP 8.9.2.
- Automatic IR discovery is physically impossible (one-way) → semi-automatic with visual confirmation.
- **Offline by default**: no core feature depends on the internet ([DATABASE.md](DATABASE.md)).

---

## Release history
The full per-version history is on the [GitHub Releases page](https://github.com/SalehGNUTUX/GT-TAHAKOM/releases)
and summarized in [CHANGELOG.md](../../CHANGELOG.md). Highlights: m0 foundation → discovery → Roku →
local IR DB → LG webOS → Samsung → manual learn → online search → touchpad → protocol converters →
experimental Android TV & Broadlink → pre-1.0 polish & CI.
