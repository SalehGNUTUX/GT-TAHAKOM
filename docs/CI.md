# CI — البناء والإصدار الآلي (GitHub Actions)

> العربية · [English](en/CI.md)

سيرَا عمل في `.github/workflows/`:

## 1. `build.yml` — بناء + lint
يعمل عند كل **دفع/طلب دمج على `main`** (ويتخطّى تغييرات التوثيق/الموقع فقط).
- JDK 17 (temurin) + Android SDK (platform android-36، build-tools 35.0.0).
- `./gradlew assembleDebug lintDebug` — يثبت أن المشروع **يُبنى من المصدر نظيفاً**.
- يرفع APK التجريبي وتقرير lint كـ artifacts.

## 2. `release.yml` — إصدار آلي
يعمل عند دفع **وسم `v*`** (مثل `v1.0.0`).
- يبني `assembleRelease`.
- يُنشئ **GitHub Release** للوسم ويُرفق الـ APK تلقائياً (مع ملاحظات مولّدة).
- الوسوم ذات لاحقة (`v1.0.0-rc1`) تُعدّ **prerelease**؛ وسم نظيف (`v1.0.0`) = إصدار كامل.

### إصدار نسخة جديدة
```bash
git tag v1.0.0 && git push origin v1.0.0
```
سير العمل يبني ويُنشئ الإصدار. (لا حاجة للبناء اليدوي المحلي بعد الآن.)

## التوقيع في CI (اختياري)
بلا أسرار → يبني release **بلا توقيع** (لا يثبَّت دون تفعيل «مصادر غير معروفة» + تجاوز
تحذير عدم التوقيع). لتوقيع آلي بمفتاحك، أضِف **Secrets** في GitHub
(`Settings → Secrets and variables → Actions`):

| السرّ | المحتوى |
| :-- | :-- |
| `KEYSTORE_BASE64` | ملف الـ keystore مُرمَّزاً base64: `base64 -w0 my.keystore` |
| `KEYSTORE_PASSWORD` | كلمة مرور المتجر |
| `KEY_ALIAS` | اسم المفتاح |
| `KEY_PASSWORD` | كلمة مرور المفتاح |

عند وجودها يعيد سير العمل بناء `keystore.properties` + ملف المفتاح ويوقّع تلقائياً
(يطابق آلية `app/build.gradle.kts`). الأسرار **لا تظهر في السجلّات** ولا تُحفظ في git.

> ملاحظة F-Droid: لا يستخدم توقيعك؛ يبني من المصدر ويوقّع بمفتاحه. توقيع CI لإصدارات
> GitHub فقط.
