package com.gnutux.tahakom.core.settings

/**
 * لغة الواجهة. [SYSTEM] يتبع لغة النظام، وإلا يُفرض اختيار المستخدم.
 * يُطبَّق عبر AppCompatDelegate (per-app locale) — انظر [LocaleManager].
 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ARABIC("ar"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}
