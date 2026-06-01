# معمارية GT-TAHAKOM

وثيقة المعمارية المرجعية. الهدف: تطبيق تحكّم متعدد البروتوكولات، نواته **طبقة نقل مجرّدة**
تختار الوسيلة المناسبة لكل جهاز — تعميمٌ لفكرة `Transmitter.getInstance()` في IRRemote.

## المبدأ الأساسي

الواجهة لا تعرف شيئاً عن البروتوكولات. تُرسل `Command` مجرّداً إلى `Transport` يختاره
`TransportRegistry` بناءً على `Device.transport`. إضافة بروتوكول جديد = صنف `Transport`
جديد يُسجَّل في `di/AppModule.kt` فقط، دون تغيير الواجهة أو النموذج.

```
UI (Compose)
   │  Command (Key / Text / IrSignal / Raw)
   ▼
TransportRegistry.forDevice(device) ──► Transport (مختار حسب النوع)
   │
   ├─ IrTransport          (ConsumerIrManager / Pronto)  ✅
   ├─ RokuTransport        (ECP / HTTP)                  ✅
   ├─ WebosTransport       (LG SSAP WebSocket)           ✅
   ├─ SamsungTizenTransport(WebSocket)                   ✅
   ├─ AndroidTvTransport   (Remote v2 + إقران TLS)       ⏳
   ├─ SonyBraviaTransport  (IRCC / REST)                 ⏳
   └─ BroadlinkTransport   (جسر WiFi-IR)                 ⏳
```

## بنية الحزم

```
com.gnutux.tahakom
├── TahakomApp.kt            @HiltAndroidApp
├── MainActivity.kt          نشاط وحيد + تنقّل + زر الرجوع
├── core/
│   ├── model/               Device, Command, ButtonId, Remote, RemoteLayouts
│   ├── transport/           Transport, TransportType, TransportResult, TransportRegistry
│   │   └── impl/            IrTransport, RokuTransport
│   ├── discovery/           Mdns/Ssdp Discovery, DiscoveryManager, ServiceFingerprint, MulticastLock
│   ├── irdb/                IrDatabase, IrDevice, Pronto, IrCommandResolver  (قاعدة assets/irdb)
│   ├── settings/            AppLanguage, LocaleManager
│   ├── share/               RemotePack, RemotePackCodec, RemotePackSharing  (.tahakom)
│   └── store/               SavedDevicesRepository  (DataStore: أجهزتي + onboarding)
├── di/                      AppModule (Hilt)
├── feature/
│   ├── onboarding/          OnboardingScreen
│   ├── devices/             DevicesScreen, AddDeviceScreen (+ ViewModels)
│   ├── irsetup/             IrSetupScreen (ضبط شبه آلي)
│   ├── remote/              RemoteScreen, MoreControls (+ ViewModel)
│   ├── settings/            SettingsScreen
│   └── share/               ImportActivity
└── ui/
    ├── theme/               Tokens (serene OKLCH→sRGB), Theme, Color, Type
    └── icons/               TahakomIcons (Material outlined), TahakomIcon
```

## قرارات تقنية مثبّتة

| القرار | القيمة | السبب |
| :-- | :-- | :-- |
| اللغة/الواجهة | Kotlin + Jetpack Compose + Material 3 | أصلي، أخف وأقوى، وصول عتادي مباشر |
| applicationId / namespace | `com.gnutux.tahakom` | متسق مع هوية GNUTUX |
| minSdk | 26 (Android 8) | تبسيط Glance/البروتوكولات الحديثة |
| compile/targetSdk | 36 | المنصة المثبّتة محلياً (android-36) |
| JDK | 17 | متطلب AGP 8.7 |
| حقن التبعيات | Hilt | معيار أندرويد الحديث |
| التخزين | Room + DataStore | الأجهزة/الريموتات + الإعدادات |
| الشبكة/التزامن | OkHttp + Coroutines/Flow | WebSocket/HTTP غير متزامن |
| نتائج النقل | `TransportResult` (Success/Failure) | تدفّق أوضح من الاستثناءات |

## النموذج الموحّد (مقتبس ومعمّم من IRRemote)

- **`Command`** (sealed): `Key(ButtonId)` المفضّل، `Text`، `IrSignal(freq, pattern)` (= `Signal` في IRRemote)، `Raw`.
- **`ButtonId`** (enum): الأزرار الدلالية (POWER، VOL_UP…) = مكافئ ثوابت `Button.java`.
- **`Device`**: محايد تجاه البروتوكول، يحمل `transport` + `address` + `metadata`.
- **`Remote`/`RemoteButton`**: تتضمّن خصائص تخطيط نسبي (x,y,w,h) للمحرر المرئي في م5.

## خطة المراحل

| المرحلة | المحتوى | الحالة |
| :-- | :-- | :-- |
| **م0** | التأسيس: Kotlin/Compose، RTL، Hilt، طبقة Transport، النموذج، IrTransport | ✅ مُنجز |
| **م1** | الاكتشاف التلقائي: NSD/mDNS + SSDP + شاشة الأجهزة الحيّة | ✅ مُنجز |
| **م2** | `RokuTransport` (ECP) + شاشة تحكّم ترسل فعلاً + إضافة من القاعدة + أجهزتي | ✅ مُنجز |
| **م4** | قاعدة IR محلية (46 جهازاً) + `IrTransport` (Pronto) + ضبط شبه آلي + قائمة "المزيد" + تعلّم يدوي | ✅ مُنجز؛ التوسيع متعدّد البروتوكولات لاحقاً |
| **م3** | توسّع الشبكة: `WebosTransport` (LG) + `SamsungTizenTransport` | ✅ LG+Samsung؛ Android TV/Sony لاحقاً |
| **التصميم** | رموز serene + أيقونات + ترحيب + شاشة تحكّم + إعدادات كاملة + سمة | 🔄 جزئي (يتبقّى Devices/AddDevice) |
| **التالي** | `AndroidTvTransport` (Remote v2) + لوحة لمس المؤشّر (pointer socket) + Sony | ⏳ |
| **م5** | جسر Broadlink + محوّلات RC5/RC6 لـ probonopd + المحرر المرئي + ويدجت + F-Droid/CI | ⏳ |

> الحالة التفصيلية الكاملة في [STATUS.md](STATUS.md). أزرار الريموت في [REMOTE_BUTTONS.md](REMOTE_BUTTONS.md). مبدأ القاعدة في [DATABASE.md](DATABASE.md).

## ما يُقتبس من IRRemote (المرجع في `_study/`)

- نموذج `Signal`(تردد+نمط نبضات Pronto) / `Button`(معرّفات دلالية) / `Remote`.
- نمط Provider لمصادر الأكواد: قاعدة محلية (`assets/db`) + قاعدة LIRC أونلاين + Learn + Manual.
- `ConsumerIrManager.transmit` في `KitKatTransmitter` → `IrTransport`.
- المحرر المرئي + الويدجت + بنية F-Droid/fastlane/CI.

**ما نتجنّبه/نحسّنه:** `consumerir required=false` (لا نحصر التطبيق بالباعث)؛ إضافة العربية/RTL (غائبة في IRRemote)؛ Kotlin بدل Java؛ وسائل نقل شبكية.
