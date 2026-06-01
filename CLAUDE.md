# CLAUDE.md — دليل العمل على GT-TAHAKOM

تطبيق أندرويد للتحكّم في التلفاز والأجهزة الإلكترونية. هذا الملف يوجّه أي جلسة Claude قادمة.

## ما هو المشروع
مركز تحكّم متعدد البروتوكولات (شبكة WiFi + IR + جسر WiFi-IR). مُلهَم معمارياً من
IRRemote (GPLv3) مع تعميم طبقة الإرسال وإضافة العربية/RTL. التفاصيل في
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) و [README.md](README.md).

## الحزمة التقنية
Kotlin + Jetpack Compose + Material 3 · Hilt · Room + DataStore · OkHttp + Coroutines.
- `applicationId` / `namespace`: `com.gnutux.tahakom`
- minSdk 26 · compile/targetSdk 36 · JDK 17 · AGP 8.9.2 · Gradle 8.11.1 (المنصة المثبّتة محلياً android-36)
- إصدارات التبعيات في `gradle/libs.versions.toml` (مصدر واحد).

## أوامر
```bash
./gradlew assembleDebug      # بناء APK تجريبي
./gradlew installDebug       # تثبيت على جهاز متصل
./gradlew lint               # فحص
```
Android SDK: `/home/gnutux/Android/Sdk` (مضبوط في `local.properties`).

## القاعدة الذهبية للمعمارية
**لإضافة بروتوكول جديد:** أنشئ صنفاً يطبّق `core/transport/Transport` تحت
`core/transport/impl/`، ثم سجّله في قائمة `di/AppModule.kt`. **لا تلمس** الواجهة أو
النموذج. الواجهة ترسل `Command` مجرّداً فقط؛ كل Transport يترجمه لبروتوكوله.

## أعراف الكود
- كل الكود بـ Kotlin. الدوال الشبكية `suspend`. النتائج عبر `TransportResult` لا الاستثناءات.
- لا نصوص ثابتة في الكود — استخدم `strings.xml` + `values-ar/strings.xml` (عربي + إنجليزي معاً دائماً).
- تعليقات بالعربية (متسقة مع المشروع الحالي).
- اتجاه RTL يتولّاه Compose تلقائياً حسب اللغة — لا تُجبر اتجاهاً.

## الحالة والخطة
- **م0 (التأسيس): ✅ منجز** — Transport/Registry، النموذج، IrTransport، Hilt، أيقونة، توثيق، Gradle wrapper.
- **م1 (الاكتشاف): ✅ منجز** — MdnsDiscovery (NsdManager) + SsdpDiscovery (UDP multicast) + DiscoveryManager + MulticastLockHolder + DiscoveryViewModel + شاشة أجهزة حيّة. اكتشاف أوفلاين بالكامل.
- **م2 (التحكّم الفعلي): 🔄 جارٍ** — RokuTransport (ECP/HTTP، إرسال حقيقي) + RemoteScreen (أزرار تعمل: طاقة/تنقّل/صوت/قنوات/وسائط) + RemoteViewModel + BrandCatalog + AddDeviceScreen (اختيار علامة + IP) + تنقّل في MainActivity (Devices/Settings/AddDevice/Remote). النقر على جهاز مكتشَف أو إضافته يدوياً يفتح ريموته.
- **التالي ضمن م2:** AndroidTvTransport (Remote v2 + إقران TLS)، ثم توسيع نطاق الأجهزة (محاكاة IRRemote: م4 IR بقاعدة assets) حسب طلب المستخدم.
- بقية المراحل (م3–م5) في docs/ARCHITECTURE.md.

## مبدأ قاعدة البيانات (موثّق في docs/DATABASE.md)
أوفلاين افتراضياً. اكتشاف الشبكة لا يحتاج قاعدة (الجهاز يُعلن عن نفسه). أكواد IR: قاعدة مدمجة في assets للعلامات الشهيرة (أوفلاين) + تنزيل اختياري لعلامة نادرة من LIRC (مرة واحدة ثم كاش) + حزم .tahakom + التعلّم. لا تجعل أي وظيفة أساسية تعتمد على الإنترنت.

## قرارات محسومة مع المستخدم
- متعدد البروتوكولات من البداية (لا IR-only).
- Kotlin/Compose (رُفض RN/Flutter لحاجة الوصول العتادي منخفض المستوى).
- `com.gnutux.tahakom` + minSdk 26 (اعتُمدت كافتراضات لم يعترض عليها المستخدم).

## مرجع IRRemote
مفكوك في `_study/IRRemote-libre/` (مُتجاهَل في git). أهم ملفاته:
`ir/io/KitKatTransmitter.java` (الإرسال)، `ir/Signal.java` (الإشارة)،
`components/Button.java` (المعرّفات الدلالية)، `providers/lirc/DBConnector.java` (قاعدة LIRC).

## ملاحظة عن البيئة
الطرفية هنا قد تُظهر إخراجاً مشوّهاً/مكرّراً بسبب مسار فيه مسافات وحروف عربية —
اعتمد أدوات Read/Write المخصّصة بدل cat/sed عند الإمكان.
