package com.gnutux.tahakom.core.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * اكتشاف الأجهزة عبر mDNS/DNS-SD باستخدام [NsdManager] المدمج في أندرويد.
 *
 * يعمل **بلا إنترنت** على الشبكة المحلية: الأجهزة تعلن عن خدماتها فنلتقطها.
 * نمسح أنواع الخدمات المعروفة (Android TV/Box، Cast، Roku) ونحوّل كلاً منها
 * عبر [ServiceFingerprint] إلى [DiscoveredDevice].
 */
class MdnsDiscovery(context: Context) : DeviceDiscovery {

    override val source = DiscoverySource.MDNS

    private val nsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    /** أنواع خدمات mDNS التي نبحث عنها (تغطّي صناديق أندرويد الأصلية والصينية + Roku). */
    private val serviceTypes = listOf(
        "_androidtvremote2._tcp.",
        "_googlecast._tcp.",
        "_roku._tcp.",
        "_airplay._tcp.",
    )

    override fun discover(): Flow<DiscoveredDevice> = callbackFlow {
        val listeners = mutableListOf<NsdManager.DiscoveryListener>()

        serviceTypes.forEach { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onServiceFound(service: NsdServiceInfo) {
                    resolve(service) { resolved ->
                        toDiscovered(resolved)?.let { trySend(it) }
                    }
                }

                override fun onServiceLost(service: NsdServiceInfo) {}
                override fun onDiscoveryStarted(serviceType: String) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            }
            listeners += listener
            runCatching {
                nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            }
        }

        awaitClose {
            listeners.forEach { runCatching { nsdManager.stopServiceDiscovery(it) } }
        }
    }

    /** يحلّ عنوان الخدمة (IP/port) — يستخدم API الحديث على أندرويد 14+ وإلا القديم. */
    private fun resolve(service: NsdServiceInfo, onResolved: (NsdServiceInfo) -> Unit) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) = onResolved(serviceInfo)
        }
        runCatching {
            @Suppress("DEPRECATION")
            nsdManager.resolveService(service, resolveListener)
        }
    }

    private fun toDiscovered(info: NsdServiceInfo): DiscoveredDevice? {
        val host = hostAddress(info) ?: return null
        val txt = info.attributes.orEmpty().mapNotNull { (k, v) ->
            v?.let { k to String(it) }
        }.toMap()

        val match = ServiceFingerprint.fromMdnsServiceType(info.serviceType, txt)
            ?: return null

        return DiscoveredDevice(
            name = info.serviceName ?: match.brand ?: "Device",
            host = host,
            port = info.port,
            transport = match.transport,
            type = match.type,
            brand = match.brand,
            model = match.model,
            source = DiscoverySource.MDNS,
            attributes = txt,
        )
    }

    @Suppress("DEPRECATION")
    private fun hostAddress(info: NsdServiceInfo): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            info.hostAddresses.firstOrNull()?.hostAddress
        } else {
            info.host?.hostAddress
        }
}
