package com.gnutux.tahakom.core.discovery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * يجمّع كل آليات الاكتشاف (mDNS + SSDP) في تدفّق واحد.
 *
 * يبثّ الأجهزة فور ظهورها من أي مصدر. يمسك MulticastLock طوال المسح ليضمن
 * استقبال حزم multicast، ويحرّره عند انتهاء الجمع. إزالة التكرار في الطبقة الأعلى
 * ([com.gnutux.tahakom.feature.devices.DiscoveryViewModel]) عبر [DiscoveredDevice.key].
 */
class DiscoveryManager(
    private val discoveries: List<DeviceDiscovery>,
    private val multicastLock: MulticastLockHolder,
) {
    /** تدفّق مدموج من كل المصادر. الجمع يبدأ المسح ويُمسك القفل تلقائياً. */
    fun discoverAll(): Flow<DiscoveredDevice> =
        merge(*discoveries.map { it.discover() }.toTypedArray())
            .onStart { multicastLock.acquire() }
            // أي خطأ شبكي (أوفلاين، تعذّر فتح سوكِت…) يُبتلَع فلا يَنهار المسح.
            .catch { }
            .onCompletion { multicastLock.release() }
}

/** مفتاح إزالة التكرار: الجهاز نفسه قد يُكتشَف عبر أكثر من مصدر. */
val DiscoveredDevice.key: String
    get() = "$host:${transport.name}"
