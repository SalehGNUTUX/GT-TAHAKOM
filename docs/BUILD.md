# البناء والتحزيم والاختبار

> العربية · [English](en/BUILD.md)

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
adb install -r release/GT-TAHAKOM-<version>-*.apk   # مثال: GT-TAHAKOM-0.9.9-online.apk
```

**يدوياً:** انقل ملف الـ APK إلى الهاتف وافتحه (فعّل "تثبيت من مصادر غير معروفة").
الإصدارات الجاهزة منشورة أيضاً على [GitHub Releases](https://github.com/SalehGNUTUX/GT-TAHAKOM/releases).

## ما يمكن اختباره

- **الاكتشاف:** زر «ابحث عن الأجهزة» → مسح حيّ (mDNS+SSDP) يكشف التلفازات الذكية وRoku/Cast.
- **التحكّم الشبكي:** LG webOS وSamsung Tizen عبر الشبكة (أول اتصال يطلب قبولاً على التلفاز).
- **التحكّم بالـ IR:** «إضافة بالاسم/الطراز» → اختر علامة → ضبط شبه آلي → جهاز تحكّم يعمل
  (يتطلّب باعث IR في الهاتف).
- **التعلّم اليدوي:** إدخال أكواد Pronto لجهاز غير مدرج (انظر [LEARN_CODES_GUIDE.md](LEARN_CODES_GUIDE.md)).
- **الإعدادات:** تبديل اللغة (عربي/إنجليزي) + السمة (فاتح/داكن/نظام).
- **المشاركة:** فتح ملف `.tahakom` → شاشة الاستيراد.

> التطبيق يعمل **بلا إنترنت** للوظائف الأساسية — يكفي WiFi بنفس شبكة الأجهزة.
