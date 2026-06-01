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
- **م0 (التأسيس): ✅ منجز** — Transport/Registry، النموذج، IrTransport، Hilt، شاشة أجهزة، أيقونة، توثيق، Gradle wrapper.
- **التالي: م1** — الاكتشاف التلقائي (NSD/mDNS + SSDP) + قائمة الأجهزة.
- بقية المراحل (م2–م5) في docs/ARCHITECTURE.md.

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
