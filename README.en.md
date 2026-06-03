<p align="center">
  <img src="GT-TAKAKOM-ICON.png" width="220" alt="GT-TAHAKOM"/>
</p>

<h1 align="center">GT-TAHAKOM — تَحَكُّمْ</h1>

<p align="center">
  A modern Android app to control your TV and electronics over multiple paths:
  the network (WiFi), Infrared (IR), and a WiFi-IR bridge.
</p>

<p align="center">
  <img src="https://img.shields.io/github/v/release/SalehGNUTUX/GT-TAHAKOM?include_prereleases&label=release&color=21E6C1&style=for-the-badge" alt="Version"/>
  <img src="https://img.shields.io/badge/License-GPLv3-FF7A1A?style=for-the-badge" alt="License"/>
  <img src="https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3ddc84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
</p>

<p align="center">
  <a href="README.md">العربية</a> · <b>English</b>
</p>

<p align="center">
  🌐 <b>Website & live demo:</b> <a href="https://salehgnutux.github.io/GT-TAHAKOM/">salehgnutux.github.io/GT-TAHAKOM</a>
</p>

---

## The idea

Unlike traditional remote apps that rely on a built-in IR blaster (rare on modern phones),
**GT-TAHAKOM** works as a **single control hub** that detects the right path for each device and uses it automatically:

| Path | Controls |
| :--- | :--- |
| 🌐 WiFi network | Android TV / Google TV, Android boxes (original & Chinese), Roku, Samsung (Tizen), LG (webOS), Sony Bravia |
| 🔴 Infrared | Any legacy IR device (on phones with a built-in emitter) |
| 📡 WiFi-IR bridge | IR devices from **any phone** via Broadlink |

> Architecture inspired by [IRRemote](https://github.com/Divested-Mobile/IRRemote) (GPLv3), generalizing the transport layer from IR-only to multiple transports, plus Arabic/RTL.

---

## Features

- **Smart device discovery:** fully automatic for network devices (mDNS/SSDP), semi-automatic for IR. **Works offline** ([docs/DISCOVERY.md](docs/DISCOVERY.md) · [docs/DATABASE.md](docs/DATABASE.md)).
- **Search by name/model** in the local database, alongside auto-discovery.
- **Online device search:** if a device isn't in the local database, search the open [probonopd/irdb](https://github.com/probonopd/irdb) catalog from inside the app — the chosen codeset is fetched, converted to Pronto on the phone, and saved locally. (The index ships in the app, so search is offline; only the download needs a connection.)
- **Touchpad** for smart TVs: swipe to navigate, tap to select.
- **Arabic + English** with instant switching from settings (per-app locale).
- **Share device packs** (`.tahakom`): share a full-brand or specific-model setup; the recipient opens the file **directly** in the app. Details in [docs/SHARING.md](docs/SHARING.md).
- Material 3 UI in the brand palette (cyan/orange), dark/light, and RTL.

---

## Current status

- **Network control ✅** — live discovery (mDNS/SSDP) + `RokuTransport` + `WebosTransport` (LG webOS/SSAP, including pointer-socket navigation) + `SamsungTizenTransport`.
- **Local IR database ✅** — 67 devices (TV/Cable/Audio) offline + real Pronto sending + semi-automatic setup with next/previous + **manual code entry**.
- **Online search ✅** — fetch remotes from the open probonopd catalog (3244-codeset index shipped) and convert to Pronto on-device (NEC/RC5/RC6/Sony/Panasonic/JVC/Mitsubishi/Denon/Pioneer/Proton).
- **Features ✅** — touchpad, language switch, `.tahakom` sharing, "My devices" list, onboarding, full settings + light/dark theme.
- **CI ✅** — GitHub Actions: build + lint on every push, automated release on tag ([docs/CI.md](docs/CI.md)).
- **Experimental 🧪** — Android TV (Remote v2) and the Broadlink bridge: included and clearly marked "under development" (need testing on real hardware).
- **Next** — app shortcuts + F-Droid publishing toward **1.0.0**. Full status in [docs/STATUS.md](docs/STATUS.md).

Full milestone plan in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Build

Requirements: JDK 17, Android SDK (platform 36, build-tools 35+).

```bash
./gradlew assembleDebug          # build a debug APK
./gradlew installDebug           # install on a connected device/emulator
```

Or open the folder directly in **Android Studio** (Ladybug or newer).
CI builds automatically on every push; tagging `v*` cuts a release ([docs/CI.md](docs/CI.md)).

---

## License

[GPLv3](LICENSE) — free and open-source software, in line with the IRRemote project it draws from.

**Developer:** GNUTUX · [github.com/SalehGNUTUX/GT-TAHAKOM](https://github.com/SalehGNUTUX/GT-TAHAKOM)
