package com.gnutux.tahakom.core.irdb

/** مدخل في فهرس قاعدة بيانات الأشعة تحت الحمراء المحلية (irdb/index.json). */
data class IrDeviceEntry(
    val category: String,
    val brand: String,
    val model: String,
    val file: String,
    val freq: Int,
    val buttons: Int,
)

/** جهاز IR كامل بأزراره (يُحمَّل عند الحاجة من ملفه في assets). */
data class IrDevice(
    val category: String,
    val brand: String,
    val model: String,
    val freq: Int,
    val buttons: List<IrButton>,
)

/**
 * زر IR: معرّف دلالي (POWER, VOL_UP...) + كود Pronto hex + التردد.
 * [label] اختياري للأوامر الخاصة (مثل أزرار HDfury) التي لا تملك معرّفاً قياسياً.
 */
data class IrButton(
    val id: String,
    val code: String,
    val freq: Int,
    val label: String? = null,
)
