package com.gnutux.tahakom.core.catalog

import com.gnutux.tahakom.core.model.DeviceType
import com.gnutux.tahakom.core.transport.TransportType

/**
 * كتالوج العلامات المعروفة — يغذّي شاشة "إضافة بالاسم/الطراز".
 *
 * مبدئياً قائمة مدمجة (أوفلاين). تتوسّع لاحقاً بأكواد IR لكل علامة (م4)
 * وبقاعدة LIRC الاختيارية. كل علامة تشير إلى وسيلة النقل المتوقّعة.
 */
object BrandCatalog {

    data class Brand(
        val name: String,
        val transport: TransportType,
        val type: DeviceType = DeviceType.TV,
        /** هل يتطلّب إدخال عنوان IP يدوياً (للأجهزة الشبكية)؟ */
        val needsAddress: Boolean = true,
    )

    /** العلامات المدعومة حالياً أو المخطّط دعمها قريباً. */
    val brands: List<Brand> = listOf(
        Brand("Roku", TransportType.ROKU),
        Brand("Android TV / Google TV", TransportType.ANDROID_TV),
        Brand("Samsung", TransportType.SAMSUNG_TIZEN),
        Brand("LG", TransportType.LG_WEBOS),
        Brand("Sony", TransportType.SONY_BRAVIA),
        // علامات IR (م4) — لا تحتاج عنواناً بل باعث IR أو جسر
        Brand("Xiaomi (IR)", TransportType.IR, needsAddress = false),
        Brand("Hisense (IR)", TransportType.IR, needsAddress = false),
        Brand("TCL (IR)", TransportType.IR, needsAddress = false),
    )

    fun byName(name: String): Brand? = brands.firstOrNull { it.name == name }
}
