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
        val group = InetAddress.getByName(MULTICAST_ADDR)
        val socket = MulticastSocket().apply {
            reuseAddress = true
            soTimeout = SOCKET_TIMEOUT_MS
        }

        try {
            val search = buildMSearch().toByteArray()
            socket.send(DatagramPacket(search, search.size, InetSocketAddress(group, SSDP_PORT)))

            val buffer = ByteArray(2048)
            // نستمر بالاستقبال حتى يُلغى التدفّق أو تنتهي مهلة السوكِت تكراراً.
            while (coroutineContext.isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (e: java.net.SocketTimeoutException) {
                    break // لا مزيد من الردود
                }
                val response = String(packet.data, 0, packet.length)
                parseResponse(response, packet.address.hostAddress)?.let { emit(it) }
            }
        } finally {
            runCatching { socket.close() }
        }
    }.flowOn(Dispatchers.IO)

    private fun buildMSearch(): String = buildString {
        append("M-SEARCH * HTTP/1.1\r\n")
        append("HOST: $MULTICAST_ADDR:$SSDP_PORT\r\n")
        append("MAN: \"ssdp:discover\"\r\n")
        append("MX: 2\r\n")
        append("ST: ssdp:all\r\n")
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
        const val SOCKET_TIMEOUT_MS = 3000
    }
}
