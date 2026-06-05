# النشر على F-Droid

> English · [العربية (هذا الملف)](FDROID.md) — انظر أيضاً [en/FDROID.md](en/FDROID.md)

هذا الدليل يوثّق كيفية إدراج **GT-TAHAKOM** في متجر **F-Droid** الحرّ. كل المتطلّبات
جاهزة في المستودع؛ يتبقّى فعل واحد لا يستطيع غير صاحب المشروع القيام به: **فتح طلب
الإدراج عبر حسابه على GitLab**.

> ✅ **حالة التقديم:** طلب RFP مفتوح بتاريخ 2026-06-04 — متابعة:
> **https://gitlab.com/fdroid/rfp/-/work_items/3972** (#3972). الباقي بانتظار متطوّعي F-Droid.

> 📘 **للمشاريع الأخرى:** هذا الملف خاص بـ GT-TAHAKOM؛ أمّا الدليل **العام القابل لإعادة الاستخدام**
> (بمتغيّرات نائبة + قسم قابل للتنفيذ آلياً) فهو في [guides/FDROID_PLAYBOOK.md](guides/FDROID_PLAYBOOK.md).

## لماذا التطبيق مؤهَّل (تحقّقنا)
- **رخصة حرّة:** GPLv3 (ملف `LICENSE`).
- **لا تبعيات احتكارية ولا متعقّبات:** لا Google Play Services ولا Firebase ولا أي SDK
  غير حرّ — فقط AndroidX/Compose/Hilt/Room/OkHttp/Coroutines (كلها حرّة).
- **يُبنى من المصدر بلا مفاتيح:** التوقيع مشروط بوجود `keystore.properties` محلياً؛ عند
  غيابه (كما في خوادم F-Droid) يبني `release` بلا توقيع، وF-Droid يوقّعه بمفتاحه. أُثبت
  هذا فعلياً عبر CI.
- **الأذونات كلّها مبرَّرة:** `INTERNET` + `ACCESS_NETWORK_STATE` + `ACCESS_WIFI_STATE`
  + `CHANGE_WIFI_MULTICAST_STATE` (لاكتشاف mDNS/SSDP) + `TRANSMIT_IR` (الأشعة).
- **بلا «ميزات مضادّة» (Anti-Features):** يعمل أوفلاين بالكامل للوظائف الأساسية؛ البحث
  الشبكي اختياري ويجلب بيانات حرّة من GitHub فقط.

## ما هو جاهز في المستودع
| العنصر | المكان |
|---|---|
| وصفة F-Droid (ملف الميتاداتا) | [`fdroid/com.gnutux.tahakom.yml`](../fdroid/com.gnutux.tahakom.yml) |
| العنوان/الوصف/سجل التغييرات (ع + EN) | `fastlane/metadata/android/{ar,en-US}/` |
| الأيقونة + 4 لقطات شاشة | `fastlane/metadata/android/en-US/images/` |
| وسم الإصدار المبنيّ منه | `v1.0.0` (versionCode 30) |

> **مهم:** صيغة بيانات fastlane التي نستخدمها هي نفسها التي يقرأها F-Droid مباشرةً من
> المستودع لجلب الوصف واللقطات تلقائياً بعد الإدراج — فلا حاجة لرفعها يدوياً.

---

## طريقتان للتقديم — اختر واحدة

### الطريق (أ) — RFP: طلب إدراج (الأبسط، مُوصى به للمرة الأولى)
يتولّى متطوّعو F-Droid كتابة/مراجعة الوصفة نيابةً عنك.

1. ادخل بحساب GitLab إلى: **https://gitlab.com/fdroid/rfp/-/issues/new**
2. اختر قالب **`Request For Packaging`** والصق النص أدناه.
3. أرسِل، ثم تابع التعليقات (قد يُطلب توضيح بسيط).

**نص الطلب (انسخه كما هو):**
```
App name: GT-TAHAKOM (تَحَكُّمْ)
Package ID: com.gnutux.tahakom
Source code: https://github.com/SalehGNUTUX/GT-TAHAKOM
License: GPL-3.0-only
Latest tag/release: v1.0.0 (versionCode 30)
Website: https://salehgnutux.github.io/GT-TAHAKOM/

Description:
A universal remote for TVs and electronics over WiFi, Infrared (IR), and a
WiFi-IR bridge. Offline-first, bilingual (Arabic/English) with RTL. Abstract
Transport layer (Roku ECP, LG webOS SSAP, Samsung Tizen, IR via Pronto, plus
experimental Android TV & Broadlink). Local IR database + on-device online
search with on-phone Pronto conversion.

FOSS check:
- No Google Play Services / Firebase / non-free SDKs.
- Builds from source with no keystore (signing is conditional; F-Droid signs).
- Permissions: INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE,
  CHANGE_WIFI_MULTICAST_STATE (mDNS/SSDP discovery), TRANSMIT_IR (IR blaster).
- No anti-features; fully offline for core features.

A ready F-Droid build recipe is in the repo at fdroid/com.gnutux.tahakom.yml
and fastlane metadata is under fastlane/metadata/android/.
```

### الطريق (ب) — Merge Request مباشر إلى fdroiddata (أسرع، لمن يريد التحكّم)
الوصفة جاهزة عندنا، فهذا الطريق سريع إن كان لديك بيئة `fdroidserver`.

```bash
# 1) انسخ مستودع البيانات (عبر fork على GitLab ثم clone للنسخة المنسوخة)
git clone https://gitlab.com/<حسابك>/fdroiddata.git
cd fdroiddata

# 2) ضع الوصفة في مكانها (من جذر مستودعنا)
cp /path/to/GT-TAHAKOM/fdroid/com.gnutux.tahakom.yml metadata/com.gnutux.tahakom.yml

# 3) تحقّق من الصياغة، ثم جرّب بناءها فعلياً
fdroid lint com.gnutux.tahakom
fdroid build -v -l com.gnutux.tahakom     # يتطلّب Docker/بيئة fdroidserver

# 4) ادفع وافتح Merge Request على gitlab.com/fdroid/fdroiddata
git checkout -b add-com.gnutux.tahakom
git add metadata/com.gnutux.tahakom.yml
git commit -m "New app: GT-TAHAKOM (com.gnutux.tahakom)"
git push -u origin add-com.gnutux.tahakom
```
ثم افتح MR من فرعك إلى `master` في `gitlab.com/fdroid/fdroiddata` ووصّفه باختصار.

---

## بعد القبول
- يظهر التطبيق في F-Droid خلال دورة البناء التالية، موقّعاً **بمفتاح F-Droid** (يختلف عن
  توقيعنا — لذا تثبيت نسخة F-Droid يتطلّب إزالة نسختنا المثبّتة يدوياً، أو العكس).
- **التحديثات تلقائية:** `UpdateCheckMode: Tags` + `AutoUpdateMode: Version v%v`. يكفي
  بعد كل إصدار:
  1. رفع `versionCode`/`versionName` في `app/build.gradle.kts`.
  2. إضافة `fastlane/metadata/android/{ar,en-US}/changelogs/<versionCode>.txt`.
  3. دفع وسم `vX.Y.Z` — يلتقطه روبوت F-Droid ويبني الجديد دون تدخّل.

## مراجع
- دليل إدراج تطبيق: https://f-droid.org/docs/Inclusion_How-To/
- مرجع حقول الميتاداتا: https://f-droid.org/docs/Build_Metadata_Reference/
- مستودع البيانات: https://gitlab.com/fdroid/fdroiddata
