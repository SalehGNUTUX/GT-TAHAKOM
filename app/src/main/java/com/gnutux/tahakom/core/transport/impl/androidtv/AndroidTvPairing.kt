package com.gnutux.tahakom.core.transport.impl.androidtv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket

/** قراءة/كتابة رسائل length-delimited (بادئة varint للطول) على تيّار. */
internal object AtvFrames {
    fun write(out: OutputStream, msg: ByteArray) {
        var len = msg.size
        val pre = java.io.ByteArrayOutputStream()
        while (true) {
            val b = len and 0x7F; len = len ushr 7
            if (len != 0) pre.write(b or 0x80) else { pre.write(b); break }
        }
        out.write(pre.toByteArray()); out.write(msg); out.flush()
    }

    fun read(inp: InputStream): ByteArray? {
        var len = 0; var shift = 0
        while (true) {
            val b = inp.read(); if (b < 0) return null
            len = len or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        val buf = ByteArray(len); var off = 0
        while (off < len) { val r = inp.read(buf, off, len - off); if (r < 0) return null; off += r }
        return buf
    }
}

/**
 * جلسة إقران Android TV (المنفذ 6467، TLS): تتبادل request/option/configuration، فيعرض
 * التلفاز رمزاً من 6 خانات، ثم يُرسل العميل السرّ المحسوب من الرمز. تبقى الجلسة مفتوحة
 * بين [start] و[submitCode] لانتظار إدخال المستخدم.
 *
 * **تجريبي وغير مُختبَر على جهاز** — أرقام الحقول وتسلسل polo من المواصفة المفتوحة.
 */
class AndroidTvPairing {

    companion object {
        private const val PORT = 6467
        private const val STATUS_OK = 200
        // أنواع ترميز polo: HEXADECIMAL = 3 ؛ دور الإدخال ROLE_TYPE_INPUT = 1.
        private const val ENC_HEX = 3
        private const val ROLE_INPUT = 1
    }

    private var socket: SSLSocket? = null
    private var serverCert: X509Certificate? = null

    /** يبدأ الإقران حتى عرض التلفاز للرمز. يُرجع true إن وصلنا لمرحلة إدخال الرمز. */
    suspend fun start(host: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val s = AndroidTvCrypto.socketFactory().createSocket(host, PORT) as SSLSocket
            s.soTimeout = 8000
            s.startHandshake()
            socket = s
            serverCert = s.session.peerCertificates.firstOrNull() as? X509Certificate
            val out = s.outputStream; val inp = s.inputStream

            // 1) PairingRequest { service_name=1, client_name=2 }
            val req = ProtoWriter().messageField(
                10, ProtoWriter().stringField(1, "androidtvremote2").stringField(2, "GT-TAHAKOM"),
            )
            AtvFrames.write(out, envelope(req)); AtvFrames.read(inp) ?: return@runCatching false

            // 2) PairingOption { input_encodings=1{type=1,symbol_length=2}, preferred_role=2 }
            val enc = ProtoWriter().int32Field(1, ENC_HEX).int32Field(2, 6)
            val opt = ProtoWriter().messageField(20, ProtoWriter().messageField(1, enc).int32Field(2, ROLE_INPUT))
            AtvFrames.write(out, envelope(opt)); AtvFrames.read(inp) ?: return@runCatching false

            // 3) PairingConfiguration { encoding=1{...}, client_role=2 } → التلفاز يعرض الرمز
            val cfg = ProtoWriter().messageField(
                30, ProtoWriter().messageField(1, ProtoWriter().int32Field(1, ENC_HEX).int32Field(2, 6)).int32Field(2, ROLE_INPUT),
            )
            AtvFrames.write(out, envelope(cfg)); AtvFrames.read(inp) ?: return@runCatching false
            true
        }.getOrElse { cancel(); false }
    }

    /** يرسل السرّ المحسوب من الرمز ويتحقّق من القبول. */
    suspend fun submitCode(code: String): Boolean = withContext(Dispatchers.IO) {
        val s = socket ?: return@withContext false
        runCatching {
            val server = serverCert ?: return@runCatching false
            val secret = AndroidTvCrypto.pairingSecret(AndroidTvCrypto.clientCertificate(), server, code.trim())
                ?: return@runCatching false
            // 4) PairingSecret { secret=1 }
            val msg = ProtoWriter().messageField(40, ProtoWriter().bytesField(1, secret))
            AtvFrames.write(s.outputStream, envelope(msg))
            val resp = AtvFrames.read(s.inputStream) ?: return@runCatching false
            // نجاح إن حملت الاستجابة حالة OK وحقل pairing_secret_ack (41).
            val map = ProtoReader.parse(resp)
            val status = (map[2]?.firstOrNull() as? Long)?.toInt()
            status == STATUS_OK && map.containsKey(41)
        }.getOrElse { false }.also { cancel() }
    }

    fun cancel() {
        runCatching { socket?.close() }
        socket = null; serverCert = null
    }

    /** يغلّف رسالة polo: protocol_version=1(=2)، status=2(=OK)، + جسم الرسالة. */
    private fun envelope(body: ProtoWriter): ByteArray {
        val w = ProtoWriter().int32Field(1, 2).int32Field(2, STATUS_OK)
        // ندمج بايتات الجسم كما هي (الجسم أصلاً حقل برقمه الصحيح 10/20/30/40).
        return w.toByteArray() + body.toByteArray()
    }
}
