package com.gnutux.tahakom.core.share

/**
 * حزمة ريموت قابلة للمشاركة (ملف `.tahakom`).
 *
 * تتيح مشاركة إعداد علامة كاملة (مثل "كل تلفازات Samsung") أو طراز محدّد.
 * عند فتح الملف على جهاز فيه التطبيق، يستوردها مباشرة (انظر `ImportActivity`).
 *
 * الصيغة JSON بسيطة وثابتة الإصدار ([schema]) لضمان التوافق المستقبلي.
 */
data class RemotePack(
    /** إصدار صيغة الملف (للترقية المستقبلية). */
    val schema: Int = CURRENT_SCHEMA,
    /** نطاق الحزمة: علامة كاملة أو طراز محدّد. */
    val scope: PackScope,
    /** اسم العلامة (Samsung, LG, Xiaomi...). */
    val brand: String,
    /** الطراز — مطلوب فقط عند [PackScope.MODEL]. */
    val model: String? = null,
    /** وصف اختياري يظهر للمستخدم قبل الاستيراد. */
    val description: String? = null,
    /** الريموتات المضمّنة (مُسلسَلة كـ JSON خام للمرونة بين الإصدارات). */
    val remotesJson: List<String> = emptyList(),
    /** من أنشأ الحزمة (اختياري، لإسناد الفضل). */
    val author: String? = null,
) {
    companion object {
        const val CURRENT_SCHEMA = 1

        /** امتداد الملف ونوع MIME المخصّص — يربطه الـ intent-filter بالتطبيق. */
        const val FILE_EXTENSION = "tahakom"
        const val MIME_TYPE = "application/vnd.gnutux.tahakom+json"

        /** مخطط الرابط العميق: tahakom://import?... */
        const val DEEP_LINK_SCHEME = "tahakom"
        const val DEEP_LINK_HOST = "import"
    }
}

/** نطاق حزمة المشاركة. */
enum class PackScope {
    /** كل طُرز علامة معيّنة. */
    BRAND,
    /** طراز واحد محدّد. */
    MODEL,
}
