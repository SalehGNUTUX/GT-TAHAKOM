# مشاركة حِزم الريموت (`.tahakom`)

تتيح المشاركة نقل إعداد جاهز (علامة كاملة أو طراز محدّد) بين مستخدمي التطبيق.
عند نقر المستلِم على الملف المشترَك، **يفتح مباشرة في GT-TAHAKOM** ويعرض معاينة استيراد.

## صيغة الملف

ملف نصّي JSON بامتداد `.tahakom`، نوع MIME المخصّص `application/vnd.gnutux.tahakom+json`.
يمثّله [`RemotePack`](../app/src/main/java/com/gnutux/tahakom/core/share/RemotePack.kt):

```json
{
  "schema": 1,
  "scope": "BRAND",
  "brand": "Samsung",
  "model": null,
  "description": "كل تلفازات Samsung الذكية",
  "author": "GNUTUX",
  "remotes": [ { /* ريموت ... */ } ]
}
```

- **`scope`**: `BRAND` (كل طُرز علامة) أو `MODEL` (طراز واحد) — يلبّي طلب «مشاركة شاملة أو طراز مخصّص».
- **`schema`**: إصدار الصيغة؛ الملفات الأحدث من قدرة التطبيق تُرفض بأمان.
- الترميز/التحليل في [`RemotePackCodec`](../app/src/main/java/com/gnutux/tahakom/core/share/RemotePackCodec.kt) (يعتمد `org.json` المدمج، بلا تبعيات).

## كيف يفتح «مباشرة في تطبيقنا»؟

`ImportActivity` تعلن ثلاثة `intent-filter` في ال*manifest*:

1. **فتح ملف** — مطابقة نوع MIME المخصّص + نمط الامتداد `.*\.tahakom` (لمزوّدات لا تعرف النوع).
2. **رابط عميق** — `tahakom://import?data=<base64>` (حزمة مضمّنة في الرابط نفسه، تُشارَك كنص/رابط).
3. **استقبال مشاركة** — `ACTION_SEND` بنوع MIME المخصّص.

كل الحالات يقرؤها [`RemotePackSharing.readFromIntent`](../app/src/main/java/com/gnutux/tahakom/core/share/RemotePackSharing.kt).

## التصدير

`RemotePackSharing.exportToShareIntent` يكتب الحزمة في ملف مؤقت بالكاش ويقدّمه عبر
`FileProvider` (سلطة `${applicationId}.fileprovider`) ثم يبني `ACTION_SEND` — فيظهر التطبيق
في ورقة المشاركة لدى المستخدم.

## ما تبقّى (مراحل لاحقة)

- ربط زر «استيراد» بحفظ الحزمة في مستودع الريموتات (Room) — مع م2.
- شاشة تصدير في الواجهة تختار النطاق (علامة/طراز) وتستدعي `exportToShareIntent`.
- التحقق من سلامة الحزمة (حدود الحجم/عدد الريموتات) قبل الاستيراد.
