package com.gnutux.tahakom.core.model

import com.gnutux.tahakom.core.transport.TransportType

/**
 * جهاز قابل للتحكّم (تلفاز، ريسيفر، مكبّر صوت، مكيّف...).
 *
 * الجهاز محايد تجاه وسيلة النقل: يحمل فقط ما يلزم لتحديده والوصول إليه،
 * بينما يتولّى [com.gnutux.tahakom.core.transport.Transport] تفاصيل البروتوكول.
 */
data class Device(
    /** معرّف فريد وثابت (عنوان MAC أو UUID أو معرّف الجسر). */
    val id: String,
    /** اسم العرض الذي يراه المستخدم. */
    val name: String,
    val type: DeviceType,
    /** وسيلة النقل المستخدمة للوصول إلى هذا الجهاز. */
    val transport: TransportType,
    /** العنوان حسب وسيلة النقل: IP للشبكة، MAC/كود للـ IR، معرّف للجسر. */
    val address: String? = null,
    /** بيانات إضافية خاصة بالبروتوكول (token، port، model، إلخ). */
    val metadata: Map<String, String> = emptyMap(),
)

/** نوع الجهاز — يُستخدم لاختيار الأيقونة والريموت الافتراضي. */
enum class DeviceType {
    TV,
    SET_TOP_BOX,
    AUDIO,
    AC,
    PROJECTOR,
    SMART_PLUG,
    OTHER,
}
