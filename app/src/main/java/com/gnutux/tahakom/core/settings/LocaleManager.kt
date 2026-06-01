package com.gnutux.tahakom.core.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * يطبّق لغة الواجهة على مستوى التطبيق (per-app language).
 *
 * يعتمد آلية AndroidX الرسمية: على أندرويد 13+ يستخدم النظام مباشرة،
 * وعلى ما دونه يخزّنها AppCompat تلقائياً ويعيد إنشاء الأنشطة.
 * التبديل من الإعدادات يستدعي [apply] فقط — لا حاجة لإعادة تشغيل يدوية.
 */
object LocaleManager {

    fun apply(language: AppLanguage) {
        val locales = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** اللغة المطبّقة حالياً (للعرض في الإعدادات). */
    fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) AppLanguage.SYSTEM
        else AppLanguage.fromTag(locales[0]?.language)
    }
}
