package com.gnutux.tahakom.core.store

import android.content.Context

/**
 * مخزن مفاتيح/رموز إقران عام لكل بروتوكول شبكي (token لكل host).
 * يُستخدم لـ Samsung (token)، Android TV (مفتاح/شهادة)، إلخ.
 * بعد قبول المستخدم الإقران مرة، يُحفظ الرمز فلا يتكرّر الطلب.
 */
class SimpleTokenStore(context: Context, prefsName: String) {

    private val prefs = context.applicationContext
        .getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun get(host: String): String? = prefs.getString(host, null)

    fun put(host: String, value: String) {
        prefs.edit().putString(host, value).apply()
    }
}
