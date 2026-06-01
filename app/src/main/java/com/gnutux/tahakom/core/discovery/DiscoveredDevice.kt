package com.gnutux.tahakom.core.discovery

import com.gnutux.tahakom.core.model.DeviceType
import com.gnutux.tahakom.core.transport.TransportType

/**
 * جهاز اكتُشِف على الشبكة المحلية قبل أن يضيفه المستخدم.
 *
 * ينتج عن آليات الاكتشاف (mDNS/SSDP) وقد يحمل تخميناً للنوع والبروتوكول
 * استُنتج من بيانات الإعلان (service type, modelName, manufacturer).
 */
data class DiscoveredDevice(
    val name: String,
    val host: String,
    val port: Int,
    /** البروتوكول المُستنتَج من نوع الخدمة المُعلَنة. */
    val transport: TransportType,
    val type: DeviceType = DeviceType.OTHER,
    /** العلامة التجارية إن أمكن استنتاجها (Samsung, LG, Sony, Roku...). */
    val brand: String? = null,
    val model: String? = null,
    /** آلية الاكتشاف التي وجدت الجهاز. */
    val source: DiscoverySource,
    /** بيانات الإعلان الخام (TXT records / SSDP headers) للتشخيص. */
    val attributes: Map<String, String> = emptyMap(),
)

/** آلية الاكتشاف. */
enum class DiscoverySource {
    MDNS,   // NSD / Bonjour
    SSDP,   // UPnP
    MANUAL, // أدخله المستخدم يدوياً
}
