package com.gnutux.tahakom.core.discovery

import kotlinx.coroutines.flow.Flow

/**
 * آلية اكتشاف أجهزة على الشبكة. كل تقنية (mDNS، SSDP) تطبّق هذه الواجهة،
 * ويجمع [DiscoveryManager] نتائجها في تدفّق واحد.
 *
 * التدفّق متواصل: يبثّ كل جهاز فور اكتشافه (لا ينتظر انتهاء المسح).
 */
interface DeviceDiscovery {
    val source: DiscoverySource

    /** يبدأ المسح ويبثّ الأجهزة فور ظهورها حتى يُلغى جمع التدفّق. */
    fun discover(): Flow<DiscoveredDevice>
}
