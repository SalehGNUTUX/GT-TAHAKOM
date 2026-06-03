# GT-TAHAKOM architecture

> English · [العربية](../ARCHITECTURE.md)

The reference architecture document. Goal: a multi-protocol remote app whose core is an
**abstract transport layer** that picks the right path for each device — a generalization of
`Transmitter.getInstance()` from IRRemote.

## Core principle

The UI knows nothing about protocols. It sends an abstract `Command` to a `Transport` chosen by
`TransportRegistry` based on `Device.transport`. Adding a new protocol = a new `Transport` class
registered in `di/AppModule.kt` only, without touching the UI or the model.

```
UI (Compose)
   │  Command (Key / Text / IrSignal / Raw)
   ▼
TransportRegistry.forDevice(device) ──► Transport (chosen by type)
   │
   ├─ IrTransport          (ConsumerIrManager / Pronto)  ✅
   ├─ RokuTransport        (ECP / HTTP)                  ✅
   ├─ WebosTransport       (LG SSAP WebSocket)           ✅
   ├─ SamsungTizenTransport(WebSocket)                   ✅
   ├─ AndroidTvTransport   (Remote v2 + TLS pairing)     🧪 experimental
   ├─ BroadlinkTransport   (WiFi-IR bridge)              🧪 experimental
   └─ SonyBraviaTransport  (IRCC / REST)                 ⏳
```

## Package structure

```
com.gnutux.tahakom
├── TahakomApp.kt            @HiltAndroidApp
├── MainActivity.kt          single activity + navigation + back handling
├── core/
│   ├── model/               Device, Command, ButtonId, Remote, RemoteLayouts
│   ├── transport/           Transport, TransportType, TransportResult, TransportRegistry
│   │   └── impl/            IrTransport, RokuTransport, Webos/Samsung/AndroidTv/Broadlink…
│   ├── discovery/           Mdns/Ssdp Discovery, DiscoveryManager, ServiceFingerprint, MulticastLock
│   ├── irdb/                IrDatabase, IrDevice, Pronto, IrCommandResolver (+ online/ search & converter)
│   ├── settings/            AppLanguage, LocaleManager
│   ├── share/               RemotePack, RemotePackCodec, RemotePackSharing  (.tahakom)
│   └── store/               SavedDevicesRepository  (DataStore: my devices + onboarding)
├── di/                      AppModule (Hilt)
├── feature/
│   ├── onboarding/          OnboardingScreen
│   ├── devices/             DevicesScreen, AddDeviceScreen, ScanScreen, AddNetworkScreen (+ ViewModels)
│   ├── irsetup/             IrSetupScreen (semi-automatic setup)
│   ├── online/              OnlineSearchScreen (probonopd online search)
│   ├── androidtv/           AndroidTvPairScreen (experimental)
│   ├── remote/              RemoteScreen, MoreControls (+ ViewModel)
│   ├── settings/            SettingsScreen
│   └── share/               ImportActivity
└── ui/
    ├── theme/               Tokens (serene OKLCH→sRGB), Theme, Color, Type
    └── icons/               TahakomIcons (Material outlined), TahakomIcon
```

## Fixed technical decisions

| Decision | Value | Why |
| :-- | :-- | :-- |
| Language/UI | Kotlin + Jetpack Compose + Material 3 | native, lighter & stronger, direct hardware access |
| applicationId / namespace | `com.gnutux.tahakom` | consistent with the GNUTUX identity |
| minSdk | 26 (Android 8) | simplifies Glance/modern protocols |
| compile/targetSdk | 36 | the locally installed platform (android-36) |
| JDK | 17 | required by AGP |
| Dependency injection | Hilt | modern Android standard |
| Storage | Room + DataStore | devices/remotes + settings |
| Network/concurrency | OkHttp + Coroutines/Flow | async WebSocket/HTTP |
| Transport results | `TransportResult` (Success/Failure) | clearer flow than exceptions |

## The unified model (borrowed & generalized from IRRemote)

- **`Command`** (sealed): preferred `Key(ButtonId)`, `Text`, `IrSignal(freq, pattern)` (= `Signal` in IRRemote), `Raw`.
- **`ButtonId`** (enum): semantic buttons (POWER, VOL_UP…) = equivalent of `Button.java` constants.
- **`Device`**: protocol-neutral, carries `transport` + `address` + `metadata`.
- **`Remote`/`RemoteButton`**: include relative-layout properties (x,y,w,h) for a future visual editor.

## Milestone plan

| Milestone | Content | Status |
| :-- | :-- | :-- |
| **m0** | Foundation: Kotlin/Compose, RTL, Hilt, transport layer, model, IrTransport | ✅ done |
| **m1** | Auto-discovery: NSD/mDNS + SSDP + live devices screen | ✅ done |
| **m2** | `RokuTransport` (ECP) + a remote screen that really sends + add-from-DB + my devices | ✅ done |
| **m4** | Local IR database (67 devices) + `IrTransport` (Pronto) + semi-auto setup + "More" menu + manual learn + online search | ✅ done |
| **m3** | Network expansion: `WebosTransport` (LG) + `SamsungTizenTransport` | ✅ LG+Samsung; Sony later |
| **Design** | serene tokens + icons + onboarding + remote + settings + devices + add-device | ✅ done (all screens on the serene theme) |
| **Experimental** | `AndroidTvTransport` (Remote v2) + `BroadlinkTransport` (WiFi-IR bridge) | 🧪 added, "under development" (need real-hardware testing) |
| **m5** | App shortcuts + F-Droid publishing + CI ✅ + visual editor + widget | ⏳ (CI done) |

> Full detailed status in [STATUS.md](STATUS.md). Remote buttons in [REMOTE_BUTTONS.md](../REMOTE_BUTTONS.md). Database principle in [DATABASE.md](DATABASE.md).

## What's borrowed from IRRemote (reference in `_study/`)

- The `Signal` (frequency + Pronto pulse pattern) / `Button` (semantic IDs) / `Remote` model.
- A Provider pattern for code sources: local DB (`assets/db`) + online LIRC DB + Learn + Manual.
- `ConsumerIrManager.transmit` in `KitKatTransmitter` → `IrTransport`.
- The visual editor + widget + F-Droid/fastlane/CI structure.

**What we avoid/improve:** `consumerir required=false` (don't restrict the app to a blaster); adding Arabic/RTL (absent in IRRemote); Kotlin instead of Java; network transports.
