# البناء والتحزيم والاختبار

## بناء سريع (تطوير)

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk (~18 MB، غير مُصغّر)
./gradlew installDebug       # تثبيت مباشر على جهاز/محاكي متصل
```

## بناء إصدار موقّع (للاختبار/التوزيع)

النسخة الموقّعة مُصغّرة بـ R8 وتزيل الموارد غير المستخدمة (~1.6 MB).

```bash
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk
```

### التوقيع
يُقرأ من `keystore.properties` في جذر المشروع (**غير مرفوع إلى git**):

```properties
storeFile=gt-tahakom-release.jks
storePassword=********
keyAlias=gt-tahakom
keyPassword=********
```

إن غاب الملف، يبني المشروع دون توقيع release (للبيئات التي لا تملك المفتاح).
لإنشاء keystore جديد:

```bash
keytool -genkeypair -v -keystore gt-tahakom-release.jks \
  -alias gt-tahakom -keyalg RSA -keysize 2048 -validity 10000
```

> ⚠️ احتفظ بـ `gt-tahakom-release.jks` و`keystore.properties` في مكان آمن خارج git.
> فقدان المفتاح يعني عدم القدرة على تحديث التطبيق بنفس التوقيع لاحقاً.

## التثبيت للاختبار

النسخة الجاهزة في `release/GT-TAHAKOM-<version>-<milestone>-release.apk`.

**عبر USB:**
```bash
adb install -r release/GT-TAHAKOM-0.1.0-m1-release.apk
```

**يدوياً:** انقل ملف الـ APK إلى الهاتف وافتحه (فعّل "تثبيت من مصادر غير معروفة").

## ما يمكن اختباره في هذه المرحلة (م1)

- فتح التطبيق → شاشة الأجهزة بالأيقونة والهوية (سماوي/برتقالي، داكن).
- زر **ابحث عن الأجهزة** → مسح حيّ لشبكة WiFi (mDNS + SSDP). يجب أن تظهر أي
  تلفازات ذكية / صناديق أندرويد / أجهزة Roku/Cast على نفس الشبكة، مصنّفة تلقائياً.
- **الإعدادات** → تبديل اللغة بين العربية والإنجليزية فورياً (مع تغيّر الاتجاه RTL/LTR).
- فتح ملف `.tahakom` (إن توفّر) → يفتح شاشة الاستيراد.

> ملاحظة: الإرسال الفعلي للأوامر (التحكّم) يأتي في م2. هذه المرحلة للاكتشاف والواجهة.
> التطبيق يعمل **بلا إنترنت** — يكفي اتصال WiFi بنفس شبكة الأجهزة.
