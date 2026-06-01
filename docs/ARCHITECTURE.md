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
   ├─ IrTransport          (ConsumerIrManager)         [م4]
   ├─ AndroidTvTransport   (Remote v2 + إقران TLS)      [م2]
   ├─ RokuTransport        (ECP / HTTP)                 [م2]
   ├─ SamsungTizenTransport(WebSocket)                  [م3]
   ├─ WebosTransport       (SSAP WebSocket)             [م3]
   ├─ SonyBraviaTransport  (IRCC / REST)                [م3]
   └─ BroadlinkTransport   (جسر WiFi-IR)                [م4]
```

## بنية الحزم

```
com.gnutux.tahakom
├── TahakomApp.kt            @HiltAndroidApp
├── MainActivity.kt          نشاط وحيد يستضيف Compose
├── core/
│   ├── model/               Device, DeviceType, Command, ButtonId, Remote, RemoteButton
│   └── transport/           Transport, TransportType, TransportResult, TransportError, TransportRegistry
│       └── impl/            IrTransport  (+ بقية الوسائل لاحقاً)
├── di/                      AppModule (Hilt) — يبني TransportRegistry
├── feature/
│   └── devices/             DevicesScreen  (+ remote/, settings/ لاحقاً)
└── ui/theme/                Color, Theme, Type
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
| **م2** | `RokuTransport` (ECP) عامل + شاشة ريموت ترسل فعلاً + كتالوج علامات + إضافة يدوية بالـ IP. `AndroidTvTransport` لاحقاً | 🔄 جارٍ |
| **م3** | توسّع الشبكة: Samsung + LG + Sony | ⏳ |
| **م4** | IR: `IrTransport` كامل + `BroadlinkTransport` + استيراد LIRC/قاعدة محلية | ⏳ |
| **م5** | المحرر المرئي + ويدجت Glance + سمات + خطوط عربية + تحزيم F-Droid/APK + CI | ⏳ |

## ما يُقتبس من IRRemote (المرجع في `_study/`)

- نموذج `Signal`(تردد+نمط نبضات Pronto) / `Button`(معرّفات دلالية) / `Remote`.
- نمط Provider لمصادر الأكواد: قاعدة محلية (`assets/db`) + قاعدة LIRC أونلاين + Learn + Manual.
- `ConsumerIrManager.transmit` في `KitKatTransmitter` → `IrTransport`.
- المحرر المرئي + الويدجت + بنية F-Droid/fastlane/CI.

**ما نتجنّبه/نحسّنه:** `consumerir required=false` (لا نحصر التطبيق بالباعث)؛ إضافة العربية/RTL (غائبة في IRRemote)؛ Kotlin بدل Java؛ وسائل نقل شبكية.
