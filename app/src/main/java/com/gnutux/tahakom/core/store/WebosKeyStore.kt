package com.gnutux.tahakom.core.store

import android.content.Context

/**
 * يحفظ مفاتيح إقران LG webOS (client-key) لكل تلفاز (حسب الـ host).
 * بعد قبول المستخدم الإقران مرة، يُحفظ المفتاح فلا يتكرّر الطلب.
 *
 * SharedPreferences بسيط ومتزامن (يُقرأ داخل جلسة WebSocket على خيط IO).
 */
class WebosKeyStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("webos_keys", Context.MODE_PRIVATE)

    fun get(host: String): String? = prefs.getString(host, null)

    fun put(host: String, key: String) {
        prefs.edit().putString(host, key).apply()
    }
}
