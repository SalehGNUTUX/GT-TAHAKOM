package com.gnutux.tahakom.core.discovery

import com.gnutux.tahakom.core.model.DeviceType
import com.gnutux.tahakom.core.transport.TransportType

/**
 * "البصمة" التي تحوّل إعلان شبكة خام (نوع خدمة mDNS أو ترويسة SSDP)
 * إلى استنتاج: أي بروتوكول؟ أي نوع جهاز؟ أي علامة؟
 *
 * هذا قلب "الاكتشاف الذكي": بمجرد أن يعلن الجهاز عن نفسه، نحدّد كيفية التحكّم به
 * **دون تدخّل المستخدم**. (الأجهزة الشبكية تعلن؛ أجهزة IR لا تعلن — انظر docs/DISCOVERY.md.)
 */
object ServiceFingerprint {

    /**
     * أنواع خدمات mDNS المعروفة → البروتوكول/النوع المقابل.
     * تغطّي: Android TV/Box (أصلي وصيني يعمل بأندرويد)، Roku، Samsung، LG، Sony.
     */
    fun fromMdnsServiceType(serviceType: String, txt: Map<String, String> = emptyMap()): Match? {
        val t = serviceType.lowercase().trimEnd('.')
        return when {
            // Android TV / Google TV / صناديق أندرويد (أصلية وصينية)
            t.contains("androidtvremote2") || t.contains("androidtvremote") ->
                Match(TransportType.ANDROID_TV, DeviceType.TV, brandFromTxt(txt))
            t.contains("googlecast") ->
                Match(TransportType.ANDROID_TV, DeviceType.TV, brandFromTxt(txt) ?: "Google")
            // Roku
            t.contains("roku") ->
                Match(TransportType.ROKU, DeviceType.TV, "Roku")
            // Samsung Tizen / Sony / LG غالباً عبر SSDP، لكن بعضها يعلن عبر mDNS أيضاً
            t.contains("samsung") || t.contains("airplay") && brandFromTxt(txt)?.contains("Samsung", true) == true ->
                Match(TransportType.SAMSUNG_TIZEN, DeviceType.TV, "Samsung")
            else -> null
        }
    }

    /** يستنتج من ترويسات SSDP (server / modelName / manufacturer). */
    fun fromSsdp(server: String?, manufacturer: String?, modelName: String?): Match? {
        val hay = listOfNotNull(server, manufacturer, modelName).joinToString(" ").lowercase()
        return when {
            hay.contains("samsung") -> Match(TransportType.SAMSUNG_TIZEN, DeviceType.TV, "Samsung", modelName)
            hay.contains("lg") || hay.contains("webos") -> Match(TransportType.LG_WEBOS, DeviceType.TV, "LG", modelName)
            hay.contains("sony") || hay.contains("bravia") -> Match(TransportType.SONY_BRAVIA, DeviceType.TV, "Sony", modelName)
            hay.contains("roku") -> Match(TransportType.ROKU, DeviceType.TV, "Roku", modelName)
            else -> null
        }
    }

    private fun brandFromTxt(txt: Map<String, String>): String? =
        txt["bt"] ?: txt["brand"] ?: txt["manufacturer"] ?: txt["md"]

    /** نتيجة الاستنتاج. */
    data class Match(
        val transport: TransportType,
        val type: DeviceType,
        val brand: String? = null,
        val model: String? = null,
    )
}
