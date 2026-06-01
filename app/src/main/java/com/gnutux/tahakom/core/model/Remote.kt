package com.gnutux.tahakom.core.model

/**
 * ريموت: مجموعة أزرار مرتبطة بجهاز.
 * مقتبس من `Remote`/`Button` في IRRemote مع إضافة [RemoteButton.command] المجرّد.
 */
data class Remote(
    val id: String,
    val name: String,
    val deviceId: String,
    val buttons: List<RemoteButton> = emptyList(),
)

/**
 * زر داخل [Remote]. يحمل أمراً مجرّداً + خصائص تخطيط للمحرر المرئي (مرحلة لاحقة).
 */
data class RemoteButton(
    val id: String,
    val label: String,
    val command: Command,
    /** أيقونة اختيارية (اسم رمز Material). */
    val icon: String? = null,
    // خصائص التخطيط النسبي (0..1) — تُستعمل في المحرر المرئي بالمرحلة 5.
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 1f,
    val h: Float = 1f,
)
