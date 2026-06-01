# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

تطبيق أندرويد للتحكّم في التلفاز والأجهزة الإلكترونية عبر وسائل متعددة (شبكة WiFi + IR
+ جسر WiFi-IR). الكود Kotlin + Jetpack Compose. التوثيق التفصيلي في `docs/` —
**ابدأ بـ [docs/STATUS.md](docs/STATUS.md)** (المصدر المرجعي للحالة: ما أُنجز/ما تبقّى).

## أوامر
```bash
./gradlew assembleDebug      # APK تجريبي (~غير مُصغّر)
./gradlew assembleRelease    # APK موقّع مُصغّر بـ R8 (يحتاج keystore.properties)
./gradlew installDebug       # تثبيت على جهاز/محاكي متصل
./gradlew compileDebugKotlin # تحقّق سريع من الكتلة بلا تحزيم
./gradlew lint
```
- **لا توجد اختبارات وحدة** في المشروع بعد.
- Android SDK: `/home/gnutux/Android/Sdk` (في `local.properties`). المنصة المثبّتة محلياً
  **android-36** (لا 35) → `compileSdk=targetSdk=36`، AGP 8.9.2، Gradle 8.11.1، JDK 17، minSdk 26.
- التوقيع: `signingConfig` يقرأ من `keystore.properties` بالجذر (مُتجاهَل في git). إن غاب
  يبني release بلا توقيع. الإصدارات تُنشر على GitHub Releases (prerelease) + نسخة في `release/`.
- بعد كل أمر `convert_ir_db.py`/`import_irdb.py` يُعاد توليد `app/src/main/assets/irdb/`.

## المعمارية — الجوهر

**طبقة `Transport` المجرّدة** هي قلب المشروع. الواجهة ترسل `Command` مجرّداً، و
`TransportRegistry` يختار الوسيلة المناسبة لكل `Device` حسب `device.transport`، وكل
`Transport` يترجم الأمر لبروتوكوله. هذا يعمّم فكرة `Transmitter.getInstance()` من IRRemote.

**القاعدة الذهبية لإضافة بروتوكول:** أنشئ صنفاً يطبّق `core/transport/Transport` تحت
`core/transport/impl/`، سجّله في قائمة `di/AppModule.kt`، وأضِف أزراره المدعومة في
`RemoteViewModel.supportedForTransport()`. **لا تلمس** الواجهة أو نموذج `Command`.

الوسائل المسجّلة حالياً: `IrTransport` (ConsumerIrManager/Pronto)، `RokuTransport`
(ECP/HTTP)، `WebosTransport` (LG SSAP WebSocket)، `SamsungTizenTransport` (WebSocket).
المعلّق: `AndroidTvTransport` (Remote v2 معقّد)، touchpad (pointer socket).

### تدفّقات أساسية تتطلّب قراءة عدة ملفات
- **الإرسال الفعلي:** `RemoteScreen` → `RemoteViewModel.send()`. لأجهزة IR يحمّل
  `RemoteViewModel.bind()` الجهازَ من `IrDatabase` ويترجم `ButtonId` → كود Pronto
  (`Command.Raw`) عبر `ensureLoaded()` **قبل** الإرسال (هذا ما يمنع فشل أول نقرة). للأجهزة
  الشبكية يمرّر `Command.Key` والـ Transport يترجمه. الأزرار المعروضة = `state.supported`.
- **قاعدة IR:** `assets/irdb/` (index.json + ملف/جهاز، أكواد Pronto). `IrDatabase` يدمج
  أجهزة assets (للقراءة) مع الريموتات المتعلَّمة من `LearnedRemoteStore` (بادئة `learned:`).
  مولّدة بـ `tools/convert_ir_db.py` (من IRRemote) و`tools/import_irdb.py` (من probonopd، NEC→Pronto).
- **الاكتشاف:** `discovery/` — `MdnsDiscovery` (NsdManager) + `SsdpDiscovery` (UDP
  multicast) → `DiscoveryManager` يدمجهما (مع MulticastLock) → `ServiceFingerprint` يستنتج
  البروتوكول/العلامة. أوفلاين بالكامل.
- **التنقّل:** `MainActivity` يدير الشاشات عبر `sealed interface Screen` + `BackHandler`
  (شاشة فرعية→الرئيسية، الرئيسية→نقرتان للخروج). السمة تُقرأ من DataStore قبل `TahakomTheme`.
- **المشاركة:** `core/share/` — صيغة `.tahakom` (RemotePack) + intent-filters في الـmanifest
  (فتح ملف + رابط `tahakom://` + SEND) → `ImportActivity`.

## أعراف الكود
- النتائج عبر `TransportResult` (Success/Failure) لا الاستثناءات. الدوال الشبكية `suspend`.
- لا نصوص ثابتة في الكود — `strings.xml` + `values-ar/strings.xml` (عربي + إنجليزي معاً).
  **استخدم «جهاز تحكّم» لا «ريموت»** في كل نص معروض (راجع [[feedback-gt-tahakom-terminology]]).
- التعليقات بالعربية. RTL يتولّاه Compose حسب اللغة — **لا تُجبر اتجاهاً**، عدا لوحة
  الاتجاهات (D-pad) التي تُفرَض LTR لأن المواضع فيزيائية لا لغوية.
- نظام التصميم في `ui/theme/Tokens.kt` (سمة serene، ألوان OKLCH محوّلة لـ sRGB) عبر
  `LocalTokens`/`tokens`. الأيقونات عبر `ui/icons/TahakomIcons` (Material outlined).

## مزالق معروفة (دروس مكلفة)
- `AppCompatActivity` (مطلوبة لتبديل اللغة per-app locale) **تتطلّب ثيماً يرث
  `Theme.AppCompat`** وإلا انهيار فوري عند الإقلاع — لا وقت البناء.
- **لا تكتب محلّل مسارات SVG يدوياً** لـ ImageVector: أعلام القوس الملتصقة (`1017.4`)
  تنهار وقت التشغيل. استُبدل بـ Material Icons.
- اكتشاف IR التلقائي **مستحيل فيزيائياً** (أحادي الاتجاه) → ضبط شبه آلي بمنطق حالة الطاقة
  (مطفأ→Power، مشغّل→Vol/Ch) في `IrSetupViewModel`.
- بروتوكولات probonopd معظمها RC5/RC6 لا NEC؛ محوّلنا يدعم NEC فقط حالياً.

## مرجع IRRemote (الإلهام المعماري، GPLv3)
مفكوك في `_study/IRRemote-libre/` (مُتجاهَل في git): `ir/io/KitKatTransmitter.java`،
`ir/Signal.java`، `components/Button.java`، `providers/lirc/DBConnector.java`.

## ملاحظة بيئة
المسار فيه مسافات وحروف عربية قد يشوّه إخراج الطرفية — اعتمد أدوات Read/Write بدل cat/sed.
