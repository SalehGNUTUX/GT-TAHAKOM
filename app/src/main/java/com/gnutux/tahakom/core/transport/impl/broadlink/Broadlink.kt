package com.gnutux.tahakom.core.transport.impl.broadlink

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * **تجريبي — تحت التطوير.** بروتوكول جسر Broadlink (RM-series) لإرسال أكواد IR عبر WiFi.
 *
 * التدفّق: اكتشاف بثّي (UDP) لمعرفة MAC/النوع → مصادقة (أمر 0x65 بمفتاح افتراضي) تُرجع
 * مُعرّف جلسة + مفتاح جديد → إرسال أمر 0x6a يحمل حزمة IR (مشفّرة AES-128-CBC).
 *
 * البنية من المرجع المفتوح (python-broadlink) وتحتاج تحقّقاً على جهاز فعلي.
 */
object Broadlink {

    private val DEFAULT_KEY = hex("097628343fe99e23765c1513accf8b02")
    private val DEFAULT_IV = hex("562e17996d093d28ddb3ba695a2e6f58")
    private const val PORT = 80

    data class Found(val host: String, val mac: ByteArray, val devType: Int)
    data class Session(val host: String, val mac: ByteArray, val devType: Int, val id: ByteArray, val key: ByteArray)

    @Volatile private var counter = 0

    /** اكتشاف بثّي للأجهزة؛ يُرجع المطابق لـ [targetIp] إن مُرّر، وإلا أول جهاز. */
    suspend fun discover(targetIp: String?, timeoutMs: Int = 4000): Found? = withContext(Dispatchers.IO) {
        runCatching {
            DatagramSocket().use { sock ->
                sock.broadcast = true
                sock.soTimeout = 1200
                val hello = buildHello(sock.localAddress?.address, sock.localPort)
                val bcast = InetAddress.getByName("255.255.255.255")
                sock.send(DatagramPacket(hello, hello.size, InetSocketAddress(bcast, PORT)))
                val buf = ByteArray(1024)
                val deadline = System.currentTimeMillis() + timeoutMs
                while (System.currentTimeMillis() < deadline) {
                    val pkt = DatagramPacket(buf, buf.size)
                    try { sock.receive(pkt) } catch (e: java.net.SocketTimeoutException) {
                        sock.send(DatagramPacket(hello, hello.size, InetSocketAddress(bcast, PORT))); continue
                    }
                    val ip = pkt.address.hostAddress ?: continue
                    val d = pkt.data
                    val devType = (d[0x34].toInt() and 0xFF) or ((d[0x35].toInt() and 0xFF) shl 8)
                    // MAC في الردّ معكوس (0x3a..0x3f).
                    val mac = ByteArray(6) { d[0x3f - it] }
                    if (targetIp == null || ip == targetIp) return@use Found(ip, mac, devType)
                }
                null
            }
        }.getOrNull()
    }

    /** مصادقة: أمر 0x65 بالمفتاح الافتراضي → مُعرّف جلسة + مفتاح جديد. */
    suspend fun authenticate(found: Found): Session? = withContext(Dispatchers.IO) {
        val payload = ByteArray(0x50)
        for (i in 0x04..0x12) payload[i] = 0x31 // مُعرّف عميل ثابت (15 بايت)
        payload[0x13] = 0x01; payload[0x1e] = 0x01; payload[0x2d] = 0x01
        "GT-TAHAKOM".toByteArray().copyInto(payload, 0x30)
        val resp = sendPacket(found.host, found.mac, found.devType, 0x65, ByteArray(4), DEFAULT_KEY, payload)
            ?: return@withContext null
        if (resp.size < 0x38 + 0x14) return@withContext null
        val enc = resp.copyOfRange(0x38, resp.size)
        val dec = aes(Cipher.DECRYPT_MODE, DEFAULT_KEY, enc)
        val id = dec.copyOfRange(0x00, 0x04)
        val key = dec.copyOfRange(0x04, 0x14)
        Session(found.host, found.mac, found.devType, id, key)
    }

    /** إرسال حزمة IR جاهزة (من [BroadlinkIr]) عبر أمر 0x6a. */
    suspend fun sendIr(session: Session, irPacket: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val payload = ByteArray(4) + irPacket // 0x02,0,0,0 ثم البيانات
        payload[0] = 0x02
        val resp = sendPacket(session.host, session.mac, session.devType, 0x6a, session.id, session.key, payload)
        resp != null
    }

    // ----- الإطار والتشفير -----

    private fun sendPacket(
        host: String, mac: ByteArray, devType: Int, command: Int, id: ByteArray, key: ByteArray, payload: ByteArray,
    ): ByteArray? = runCatching {
        val packet = ByteArray(0x38)
        byteArrayOf(0x5a, 0xa5.toByte(), 0xaa.toByte(), 0x55, 0x5a, 0xa5.toByte(), 0xaa.toByte(), 0x55).copyInto(packet)
        packet[0x24] = (devType and 0xFF).toByte(); packet[0x25] = ((devType shr 8) and 0xFF).toByte()
        packet[0x26] = command.toByte()
        counter = (counter + 1) and 0xFFFF
        packet[0x28] = (counter and 0xFF).toByte(); packet[0x29] = ((counter shr 8) and 0xFF).toByte()
        mac.copyInto(packet, 0x2a)
        id.copyInto(packet, 0x30)
        // تجزئة الحمولة (قبل التشفير) في 0x34..0x35.
        var cs = 0xbeaf
        for (b in payload) cs = (cs + (b.toInt() and 0xFF)) and 0xFFFF
        packet[0x34] = (cs and 0xFF).toByte(); packet[0x35] = ((cs shr 8) and 0xFF).toByte()
        val full = packet + aes(Cipher.ENCRYPT_MODE, key, pad16(payload))
        // تجزئة الحزمة كاملة في 0x20..0x21.
        var cs2 = 0xbeaf
        for (b in full) cs2 = (cs2 + (b.toInt() and 0xFF)) and 0xFFFF
        full[0x20] = (cs2 and 0xFF).toByte(); full[0x21] = ((cs2 shr 8) and 0xFF).toByte()

        DatagramSocket().use { sock ->
            sock.soTimeout = 3000
            sock.send(DatagramPacket(full, full.size, InetSocketAddress(InetAddress.getByName(host), PORT)))
            val buf = ByteArray(1024); val pkt = DatagramPacket(buf, buf.size)
            sock.receive(pkt)
            buf.copyOfRange(0, pkt.length)
        }
    }.getOrNull()

    private fun buildHello(localIp: ByteArray?, localPort: Int): ByteArray {
        val p = ByteArray(0x30)
        if (localIp != null && localIp.size == 4) {
            p[0x18] = localIp[3]; p[0x19] = localIp[2]; p[0x1a] = localIp[1]; p[0x1b] = localIp[0]
        }
        p[0x1c] = (localPort and 0xFF).toByte(); p[0x1d] = ((localPort shr 8) and 0xFF).toByte()
        p[0x26] = 0x06
        var cs = 0xbeaf
        for (b in p) cs = (cs + (b.toInt() and 0xFF)) and 0xFFFF
        p[0x20] = (cs and 0xFF).toByte(); p[0x21] = ((cs shr 8) and 0xFF).toByte()
        return p
    }

    private fun aes(mode: Int, key: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(DEFAULT_IV))
        return cipher.doFinal(data)
    }

    private fun pad16(data: ByteArray): ByteArray {
        val rem = data.size % 16
        return if (rem == 0) data else data + ByteArray(16 - rem)
    }

    private fun hex(s: String) = ByteArray(s.length / 2) { ((s[it * 2].digitToInt(16) shl 4) or s[it * 2 + 1].digitToInt(16)).toByte() }
}
