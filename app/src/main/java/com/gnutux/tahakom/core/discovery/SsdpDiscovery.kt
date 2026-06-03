package com.gnutux.tahakom.core.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import kotlin.coroutines.coroutineContext

/**
 * اكتشاف الأجهزة عبر SSDP (جزء من UPnP) — بثّ M-SEARCH عبر UDP multicast
 * واستقبال ردود الأجهزة. يكشف تلفازات Samsung/LG/Sony وأجهزة أخرى تعلن عبر UPnP.
 *
 * يعمل **بلا إنترنت** بالكامل على الشبكة المحلية (المجموعة 239.255.255.250:1900).
 */
class SsdpDiscovery : DeviceDiscovery {

    override val source = DiscoverySource.SSDP

    override fun discover(): Flow<DiscoveredDevice> = flow {
        // إنشاء السوكِت قد يفشل بلا شبكة — لا تَنهَر، أنهِ التدفّق بهدوء (مسح فارغ).
        val socket = runCatching {
            MulticastSocket().apply {
                reuseAddress = true
                soTimeout = 1200 // مهلة استقبال قصيرة لنعيد الإرسال ضمن النافذة الكلّية
            }
        }.getOrNull() ?: return@flow

        try {
            val target = InetSocketAddress(InetAddress.getByName(MULTICAST_ADDR), SSDP_PORT)
            // استعلامات M-SEARCH متعدّدة: ssdp:all + أهداف ST خاصّة بالتلفازات الذكية،
            // لأن كثيراً منها لا يردّ إلا على نوع خدمته تحديداً (LG/Samsung/Roku/DIAL).
            val queries = ST_TARGETS.map { buildMSearch(it).toByteArray() }
            fun blast() = queries.forEach { q ->
                if (!sendOnAllInterfaces(socket, q, target)) {
                    runCatching { socket.send(DatagramPacket(q, q.size, target)) }
                }
            }
            blast()

            val buffer = ByteArray(2048)
            val seen = HashSet<String>()
            val deadline = System.currentTimeMillis() + SCAN_WINDOW_MS
            var lastBlast = System.currentTimeMillis()
            // نبقى نستقبل حتى نهاية النافذة، ونعيد البثّ دورياً (UDP غير موثوق).
            while (coroutineContext.isActive && System.currentTimeMillis() < deadline) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                    val device = parseResponse(String(packet.data, 0, packet.length), packet.address.hostAddress)
                    if (device != null && seen.add("${device.host}:${device.transport.name}")) emit(device)
                } catch (e: java.net.SocketTimeoutException) {
                    // لا ردّ في هذه النافذة القصيرة — أعِد البثّ كل ~1.5s واستمر.
                    if (System.currentTimeMillis() - lastBlast > 1500) { blast(); lastBlast = System.currentTimeMillis() }
                }
            }
        } catch (e: Exception) {
            // أي خطأ شبكي (أوفلاين/تعذّر الإرسال) → أنهِ التدفّق بهدوء بلا انهيار.
        } finally {
            runCatching { socket.close() }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * يرسل M-SEARCH عبر كل واجهة شبكة فاعلة تدعم multicast (WiFi/إيثرنت). يُرجع true إن
     * أُرسِل على واحدة على الأقل. هذا يضمن وصول البحث للتلفاز على WiFi حتى مع تفعيل
     * بيانات الجوّال (التي قد تكون الواجهة الافتراضية).
     */
    private fun sendOnAllInterfaces(socket: MulticastSocket, data: ByteArray, target: InetSocketAddress): Boolean {
        var sent = false
        val ifaces = runCatching { NetworkInterface.getNetworkInterfaces() }.getOrNull() ?: return false
        for (ni in ifaces) {
            val usable = runCatching { ni.isUp && !ni.isLoopback && ni.supportsMulticast() }.getOrDefault(false)
            if (!usable) continue
            val hasIpv4 = ni.inetAddresses.toList().any { !it.isLoopbackAddress && it.address.size == 4 }
            if (!hasIpv4) continue
            runCatching {
                socket.networkInterface = ni
                socket.send(DatagramPacket(data, data.size, target))
                sent = true
            }
        }
        return sent
    }

    private fun buildMSearch(st: String): String = buildString {
        append("M-SEARCH * HTTP/1.1\r\n")
        append("HOST: $MULTICAST_ADDR:$SSDP_PORT\r\n")
        append("MAN: \"ssdp:discover\"\r\n")
        append("MX: 2\r\n")
        append("ST: $st\r\n")
        append("\r\n")
    }

    private fun parseResponse(response: String, host: String?): DiscoveredDevice? {
        host ?: return null
        val headers = response.lineSequence()
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) null
                else line.substring(0, idx).trim().uppercase() to line.substring(idx + 1).trim()
            }.toMap()

        val server = headers["SERVER"]
        val match = ServiceFingerprint.fromSsdp(
            server = server,
            manufacturer = headers["X-MANUFACTURER"],
            modelName = headers["X-MODEL"] ?: headers["ST"],
        ) ?: return null

        return DiscoveredDevice(
            name = match.brand ?: "Device",
            host = host,
            port = match.transport.defaultPort,
            transport = match.transport,
            type = match.type,
            brand = match.brand,
            model = match.model,
            source = DiscoverySource.SSDP,
            attributes = headers,
        )
    }

    private companion object {
        const val MULTICAST_ADDR = "239.255.255.250"
        const val SSDP_PORT = 1900
        const val SCAN_WINDOW_MS = 6000L // نافذة استقبال كلّية (التلفازات قد تردّ متأخّرة)
        // أهداف ST: العام + خدمات التلفازات الذكية الشائعة (تردّ بموثوقية أعلى من ssdp:all وحده).
        val ST_TARGETS = listOf(
            "ssdp:all",
            "urn:dial-multiscreen-org:service:dial:1",
            "urn:lge-com:service:webos-second-screen:1",
            "urn:samsung.com:device:RemoteControlReceiver:1",
            "urn:schemas-upnp-org:device:MediaRenderer:1",
            "roku:ecp",
        )
    }
}
