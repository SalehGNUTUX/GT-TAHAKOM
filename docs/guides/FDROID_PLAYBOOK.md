# دليل نشر تطبيق أندرويد على F-Droid — قالب عام قابل لإعادة الاستخدام

> العربية · [English](en/FDROID_PLAYBOOK.md)

دليل **مجرّد من أي مشروع** لرفع تطبيق أندرويد (Kotlin/Java + Gradle) إلى متجر **F-Droid**
الحرّ، من فحص الأهليّة حتى القبول والصيانة. صُمِّم ليُعتمَد **يدوياً** أو من **نموذج ذكاء
اصطناعي**. استبدل المتغيّرات النائبة `<…>` بقيم مشروعك.

> مثال حيّ مطبَّق على هذا الدليل: [docs/FDROID.md](../FDROID.md) و
> [fdroid/com.gnutux.tahakom.yml](../../fdroid/com.gnutux.tahakom.yml).

## المتغيّرات النائبة (املأها أولاً)
| المتغيّر | المعنى | مثال |
|---|---|---|
| `<APP_ID>` | معرّف الحزمة (applicationId) | `com.example.app` |
| `<REPO_URL>` | رابط المستودع العلني (HTTPS) | `https://github.com/user/app` |
| `<REPO_GIT>` | رابط git للاستنساخ | `https://github.com/user/app.git` |
| `<LICENSE>` | معرّف SPDX للرخصة | `GPL-3.0-only`، `MIT`، `Apache-2.0` |
| `<TAG>` | وسم الإصدار المبنيّ منه | `v1.0.0` |
| `<VNAME>` | versionName | `1.0.0` |
| `<VCODE>` | versionCode (عدد صحيح متزايد) | `30` |
| `<SUBDIR>` | مجلد وحدة التطبيق | `app` |
| `<GL_USER>` | اسم مستخدمك على GitLab | `username` |

---

## المرحلة 0 — شرط القبول (افحصها قبل أي شيء)
F-Droid يقبل **البرمجيات الحرّة فقط** التي **تُبنى من المصدر** على خوادمه. تأكّد من **كل** ما يلي:

- [ ] **رخصة حرّة** معتمدة (OSI/FSF) وملف `LICENSE` موجود؛ حدّد معرّف SPDX (`<LICENSE>`).
- [ ] **لا تبعيات/مكتبات احتكارية:** لا Google Play Services، لا Firebase/Crashlytics/Analytics،
      لا أي SDK مغلق المصدر. (هذه تُسبّب رفضاً أو وسم «ميزة مضادّة».)
- [ ] **لا متعقّبات (trackers)** ولا إعلانات. (راجع قائمة Exodus Privacy عند الشكّ.)
- [ ] **يُبنى من المصدر بلا أسرار:** البناء يجب أن ينجح **دون** ملف توقيع/مفاتيح (انظر المرحلة 2).
- [ ] **لا ثنائيات مُسبَقة (prebuilt blobs):** لا `.jar`/`.aar`/`.so` مُضمَّنة بلا مصدر؛ كل
      التبعيات من مستودعات Maven العلنية الحرّة.
- [ ] **الأذونات مبرَّرة** ومذكورة بوضوح.
- [ ] **versionCode عدد صحيح يتزايد** مع كل إصدار.

> إن وُجد جزء غير حرّ (مثل نكهة GMS)، فأنشئ **نكهة (flavor) حرّة بالكامل** وابنِ F-Droid منها،
> أو أزِل الاعتماد. خلاف ذلك يُرفَض أو يُوسَم بـ Anti-Feature (مثل `NonFreeDep`/`NonFreeNet`).

---

## المرحلة 1 — تجهيز بيانات المتجر (fastlane)
F-Droid يقرأ الوصف واللقطات **آلياً** من شجرة fastlane داخل مستودعك (إن وُجدت). أنشئها:

```
fastlane/metadata/android/
├── en-US/
│   ├── title.txt                 # اسم التطبيق (سطر واحد)
│   ├── short_description.txt      # وصف قصير (≤ 80 حرفاً يُفضّل)
│   ├── full_description.txt       # الوصف الكامل (Markdown بسيط)
│   ├── changelogs/
│   │   └── <VCODE>.txt            # سجل تغييرات هذا الإصدار (اسم الملف = versionCode)
│   └── images/
│       ├── icon.png              # أيقونة 512×512
│       └── phoneScreenshots/
│           ├── 1.png
│           └── 2.png
└── <lang>/                       # لغات إضافية (مثل ar) بنفس البنية (اختياري)
```
- اسم ملف سجل التغييرات **هو رقم `<VCODE>`** (لا اسم الإصدار).
- `en-US` هو الاحتياطي؛ اللغات الأخرى تَرِث منه ما ينقصها.
- لقطات الشاشة **اختيارية** لكنها تحسّن الإدراج كثيراً.

---

## المرحلة 2 — تجهيز إعداد البناء (الأهمّ)
الهدف: أن ينجح `assembleRelease` **بلا أي مفاتيح**، ليبنيه F-Droid ويوقّعه بمفتاحه.

**1) اجعل التوقيع مشروطاً** بوجود ملف مفاتيح محلي (مُتجاهَل في git):
```kotlin
// app/build.gradle.kts
val keystorePropsFile = rootProject.file("keystore.properties")
android {
    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") { /* يقرأ من keystore.properties */ }
        }
    }
    buildTypes {
        release {
            // وقّع فقط إن توفّر الملف؛ وإلا اتركه بلا توقيع (F-Droid يوقّع).
            if (keystorePropsFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }
}
```
**2) تجاهُل الأسرار في git** — `.gitignore`:
```
keystore.properties
*.keystore
*.jks
```
**3) لا مستودعات Maven غير حرّة** ولا `jcenter()`؛ اكتفِ بـ `google()` و`mavenCentral()`.
**4) تحقّق محلياً** أن البناء ينجح بلا مفاتيح:
```bash
rm -f keystore.properties && ./gradlew clean assembleRelease
```

---

## المرحلة 3 — كتابة وصفة F-Droid (ملف الميتاداتا)
ملف واحد باسم `<APP_ID>.yml` (سيوضَع داخل `metadata/` في مستودع fdroiddata). احتفظ بنسخة في
مشروعك تحت `fdroid/<APP_ID>.yml`:

```yaml
Categories:
  - Connectivity            # أو System, Internet, Multimedia, Games...
License: <LICENSE>          # معرّف SPDX
AuthorName: <اسمك>
AuthorEmail: <بريدك>
WebSite: <REPO_URL>
SourceCode: <REPO_URL>
IssueTracker: <REPO_URL>/issues
Changelog: <REPO_URL>/blob/HEAD/CHANGELOG.md

AutoName: <اسم التطبيق>

RepoType: git
Repo: <REPO_GIT>

Builds:
  - versionName: <VNAME>
    versionCode: <VCODE>
    commit: <TAG>           # وسم أو هاش يُبنى منه (يُفضّل وسم موقَّع)
    subdir: <SUBDIR>
    gradle:
      - yes                 # النكهة الافتراضية؛ أو ضع اسم النكهة الحرّة

AutoUpdateMode: Version v%v # عند وسم vX.Y.Z جديد يُنشئ بناءً تلقائياً (%v=الاسم، %c=الكود)
UpdateCheckMode: Tags       # يراقب وسوم git؛ بدائل: Tags <regex>, RepoManifest, HTTP, None
CurrentVersion: <VNAME>
CurrentVersionCode: <VCODE>
```
- **`AutoUpdateMode` + `UpdateCheckMode: Tags`** = تحديث آلي مدى الحياة: يكفي دفع وسم جديد.
- إن كان `commit` بلا بادئة `v`، استخدم `AutoUpdateMode: Version %v`.
- حقول مفيدة عند الحاجة: `AntiFeatures: [NonFreeNet]`، `MaintainerNotes`،
  وداخل عنصر البناء: `scandelete`/`scanignore` (لاستثناء ملفات يرفضها الفاحص)، `rm`، `prebuild`.

---

## المرحلة 4 — التحقّق المحلي (اختياري لكنه يسرّع القبول)
يتطلّب `fdroidserver` (وأحياناً Docker). إن لم يتوفّر، تجاوز إلى المرحلة 5 (مسار RFP).
```bash
# داخل نسخة من مستودع fdroiddata:
fdroid rewritemeta <APP_ID>        # توحيد تنسيق الوصفة
fdroid lint <APP_ID>               # فحص الحقول والصياغة
fdroid build -v -l <APP_ID>        # بناء فعلي (يحتاج Docker/بيئة fdroidserver)
fdroid checkupdates <APP_ID>       # محاكاة اكتشاف التحديث بالوسوم
```

---

## المرحلة 5 — التقديم (اختر مساراً)

### المسار (أ) — RFP: طلب إدراج (الأبسط، مُوصى به أول مرة)
متطوّعو F-Droid يكتبون/يراجعون الوصفة. افتح مشكلة على **https://gitlab.com/fdroid/rfp/-/issues/new**
واملأ قالب «Request For Packaging». نصّ جاهز:
```
App name: <اسم التطبيق>
Package ID: <APP_ID>
Source code: <REPO_URL>
License: <LICENSE>
Latest tag/release: <TAG> (versionCode <VCODE>)

Description:
<وصف موجز للوظيفة>

FOSS check:
- No Google Play Services / Firebase / non-free SDKs.
- Builds from source with no keystore (signing is conditional; F-Droid signs).
- Permissions: <عدّد الأذونات وسبب كلٍّ>.
- No anti-features.

A build recipe is ready at fdroid/<APP_ID>.yml and fastlane metadata under fastlane/metadata/android/.
```

**فتح RFP آلياً عبر `glab`** (بعد `glab auth login`):
```bash
glab issue create --repo fdroid/rfp \
  --title "<اسم التطبيق> (<APP_ID>)" \
  --description "$(cat rfp_body.txt)"
```
**أو رابط مُعبّأ مسبقاً** (لا يتطلّب أدوات): استخدم معاملي
`?issue[title]=…&issue[description]=…` على `…/rfp/-/issues/new` بعد ترميز URL.

### المسار (ب) — Merge Request مباشر إلى fdroiddata (أسرع، تحكّم أكبر)
```bash
# 1) انسخ (fork) مستودع البيانات إلى حسابك
glab repo fork fdroid/fdroiddata --remote=false
# 2) استنساخ سطحي (fdroiddata ضخم)
git clone --depth 1 https://gitlab.com/<GL_USER>/fdroiddata.git && cd fdroiddata
# 3) ضع الوصفة في مكانها
cp /path/to/<APP_ID>.yml metadata/<APP_ID>.yml
# 4) (إن توفّر) تحقّق
fdroid lint <APP_ID>
# 5) فرع + التزام + دفع
git checkout -b add-<APP_ID>
git add metadata/<APP_ID>.yml
git commit -m "New app: <اسم التطبيق> (<APP_ID>)"
git push -u origin add-<APP_ID>
# 6) افتح MR إلى master في fdroid/fdroiddata
glab mr create --repo fdroid/fdroiddata \
  --source-branch "<GL_USER>:add-<APP_ID>" --target-branch master \
  --title "New app: <اسم التطبيق> (<APP_ID>)" \
  --description "FOSS, builds from source. Closes fdroid/rfp#<RFP_ID> (إن وُجد)." --yes
```

---

## المرحلة 6 — بعد القبول والصيانة
- يُبنى التطبيق ويظهر في الفهرس خلال دورة البناء التالية، **موقَّعاً بمفتاح F-Droid** (يختلف عن
  مفتاحك → تثبيت نسخة F-Droid يتطلّب إزالة نسختك الموقَّعة يدوياً، والعكس).
- **التحديثات آلية** مع `UpdateCheckMode: Tags`. لكل إصدار جديد:
  1. ارفع `versionCode`/`versionName`.
  2. أضِف `fastlane/metadata/android/<lang>/changelogs/<VCODE>.txt`.
  3. ادفع وسم `v<VNAME>` → يلتقطه روبوت F-Droid ويبني الجديد بلا تدخّل.
- (متقدّم/اختياري) **البناء المُعاد إنتاجه** + `AllowedAPKSigningKeys` لمطابقة توقيعك توقيع F-Droid
  (يسمح بالتحديث بينهما دون إزالة).

---

## أسباب الرفض الشائعة (تفاديها)
- تبعية/خدمة غير حرّة (GMS، خرائط جوجل المغلقة، خطوط/أصول غير حرّة) → أزِلها أو نكهة حرّة.
- فشل البناء لاعتماده على مفتاح/أسرار أو على jcenter المهجور.
- ثنائيات مُسبَقة بلا مصدر (يرصدها الفاحص) → `scandelete`/بناء من المصدر.
- `versionCode` غير متزايد أو وسم غير موجود في `commit`.
- اتصال بخدمة شبكية غير حرّة كوظيفة أساسية → وسم `NonFreeNet` على الأقل.

---

## مرجع سريع قابل للتنفيذ آلياً (لنموذج ذكاء اصطناعي)
نفّذ بالترتيب، وتوقّف عند أول فشل واطلب تدخّلاً بشرياً:
1. **افحص الأهليّة** (المرحلة 0). إن وُجد عنصر غير حرّ → أبلِغ وتوقّف.
2. **تحقّق من البناء بلا مفاتيح:** `rm -f keystore.properties && ./gradlew clean assembleRelease`.
   فشل → أصلِح إعداد التوقيع المشروط (المرحلة 2) قبل المتابعة.
3. **استخرج** `<APP_ID>` و`<VNAME>` و`<VCODE>` من `build.gradle(.kts)`، و`<LICENSE>` من `LICENSE`،
   و`<TAG>` من `git tag --list 'v*' | tail -1`.
4. **أنشئ** `fdroid/<APP_ID>.yml` (المرحلة 3) و(إن غابت) شجرة fastlane (المرحلة 1).
5. **التقديم بشري الإذن:** فتح RFP/MR يتطلّب حساب GitLab للمستخدم — **لا تنفّذه دون إذن صريح**.
   جهّز نص RFP وأمر `glab` الجاهزَين واعرضهما، أو ولّد الرابط المُعبّأ مسبقاً.
6. **بعد القبول:** ذكّر بأن آليّة التحديث = دفع وسم جديد + ملف changelog باسم `<VCODE>`.

## روابط رسمية
- إدراج تطبيق: https://f-droid.org/docs/Inclusion_How-To/
- مرجع حقول الميتاداتا: https://f-droid.org/docs/Build_Metadata_Reference/
- مستودع البيانات: https://gitlab.com/fdroid/fdroiddata
- طلبات الإدراج (RFP): https://gitlab.com/fdroid/rfp
- أدوات fdroidserver: https://f-droid.org/docs/Installing_the_Server_and_Repo_Tools/
