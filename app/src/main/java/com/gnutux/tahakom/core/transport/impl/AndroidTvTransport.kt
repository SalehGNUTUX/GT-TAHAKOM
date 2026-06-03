package com.gnutux.tahakom.core.transport.impl

import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Command
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.transport.Transport
import com.gnutux.tahakom.core.transport.TransportError
import com.gnutux.tahakom.core.transport.TransportResult
import com.gnutux.tahakom.core.transport.TransportType
import com.gnutux.tahakom.core.transport.impl.androidtv.AndroidTvCrypto
import com.gnutux.tahakom.core.transport.impl.androidtv.AtvFrames
import com.gnutux.tahakom.core.transport.impl.androidtv.ProtoReader
import com.gnutux.tahakom.core.transport.impl.androidtv.ProtoWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket

/**
 * **تجريبي — تحت التطوير.** وسيلة Android TV / Google TV (بروتوكول Remote v2، المنفذ 6466
 * عبر TLS متبادل بشهادة عميل). يتطلّب إقراناً مسبقاً (رمز 6 خانات) عبر [androidtv.AndroidTvPairing].
 *
 * يفتح اتصالاً دائماً، ينفّذ مصافحة configure/set_active، يردّ على نبضات ping، ويُرسل
 * المفاتيح عبر RemoteKeyInject. أرقام الحقول وأكواد المفاتيح من المواصفة المفتوحة وتحتاج
 * تحقّقاً على جهاز فعلي.
 */
class AndroidTvTransport : Transport {

    override val type = TransportType.ANDROID_TV

    private var host: String? = null
    private var socket: SSLSocket? = null
    @Volatile private var reader: Thread? = null

    override suspend fun isAvailable(): Boolean = true

    override suspend fun connect(device: Device): TransportResult<Unit> = withContext(Dispatchers.IO) {
        val h = device.address ?: return@withContext TransportResult.Failure(TransportError.NOT_CONNECTED)
        if (socket?.isConnected == true && host == h) return@withContext TransportResult.Success(Unit)
        try {
            val s = AndroidTvCrypto.socketFactory().createSocket(h, 6466) as SSLSocket
            s.soTimeout = 8000
            s.startHandshake() // يفشل إن لم تُقرَن الشهادة بعد → يطلب الإقران
            // مصافحة أولية: ردّ على configure ثم set_active.
            handshake(s)
            host = h; socket = s
            startReader(s)
            TransportResult.Success(Unit)
        } catch (e: SSLHandshakeException) {
            TransportResult.Failure(TransportError.PAIRING_REQUIRED, e)
        } catch (e: Exception) {
            TransportResult.Failure(TransportError.NETWORK, e)
        }
    }

    override suspend fun send(device: Device, command: Command): TransportResult<Unit> = withContext(Dispatchers.IO) {
        val button = (command as? Command.Key)?.button
            ?: return@withContext TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)
        val keyCode = button.toAndroidKeyCode()
            ?: return@withContext TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)
        if (socket?.isConnected != true || host != device.address) {
            when (val c = connect(device)) { is TransportResult.Failure -> return@withContext c; else -> {} }
        }
        val out = socket?.takeIf { it.isConnected }?.outputStream
            ?: return@withContext TransportResult.Failure(TransportError.NOT_CONNECTED)
        try {
            // RemoteMessage{ remote_key_inject=10 { direction=1 (SHORT=3), key_code=2 } }
            val inject = ProtoWriter().int32Field(1, 3).int32Field(2, keyCode)
            val msg = ProtoWriter().messageField(10, inject)
            AtvFrames.write(out, msg.toByteArray())
            TransportResult.Success(Unit)
        } catch (e: Exception) {
            TransportResult.Failure(TransportError.NETWORK, e)
        }
    }

    override suspend fun disconnect(device: Device) {
        reader?.interrupt(); reader = null
        runCatching { socket?.close() }; socket = null; host = null
    }

    /** مصافحة البدء: configure → set_active (نقرأ رسائل الخادم ونردّ). */
    private fun handshake(s: SSLSocket) {
        val out = s.outputStream
        repeat(4) {
            val m = AtvFrames.read(s.inputStream) ?: return
            val map = ProtoReader.parse(m)
            when {
                map.containsKey(1) -> { // RemoteConfigure → ردّ بمعلومات الجهاز
                    val info = ProtoWriter().stringField(1, "GT-TAHAKOM").stringField(2, "GNUTUX")
                        .int32Field(3, 1).int32Field(4, 1).stringField(5, "com.gnutux.tahakom").stringField(6, "1.0")
                    AtvFrames.write(out, ProtoWriter().messageField(1, ProtoWriter().int32Field(1, 622).messageField(2, info)).toByteArray())
                }
                map.containsKey(2) -> { // RemoteSetActive → ردّ active
                    AtvFrames.write(out, ProtoWriter().messageField(2, ProtoWriter().int32Field(1, 622)).toByteArray())
                    return // اكتملت المصافحة
                }
            }
        }
    }

    /** خيط قراءة دائم: يردّ على نبضات ping ويتجاهل البقية. */
    private fun startReader(s: SSLSocket) {
        val t = Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    val m = AtvFrames.read(s.inputStream) ?: break
                    val map = ProtoReader.parse(m)
                    val ping = map[8]?.firstOrNull() as? ByteArray ?: continue
                    val v1 = (ProtoReader.parse(ping)[1]?.firstOrNull() as? Long) ?: 1L
                    // RemoteMessage{ remote_ping_response=9 { val1=1 } }
                    AtvFrames.write(s.outputStream, ProtoWriter().messageField(9, ProtoWriter().varintField(1, v1)).toByteArray())
                }
            } catch (_: Exception) { /* أُغلق الاتصال */ }
        }
        t.isDaemon = true; t.start(); reader = t
    }

    /** خريطة الزر الدلالي → كود مفتاح أندرويد (KEYCODE_*). */
    private fun ButtonId.toAndroidKeyCode(): Int? = when (this) {
        ButtonId.POWER -> 26
        ButtonId.HOME -> 3
        ButtonId.BACK -> 4
        ButtonId.MENU -> 82
        ButtonId.NAV_UP -> 19
        ButtonId.NAV_DOWN -> 20
        ButtonId.NAV_LEFT -> 21
        ButtonId.NAV_RIGHT -> 22
        ButtonId.NAV_OK -> 23
        ButtonId.VOL_UP -> 24
        ButtonId.VOL_DOWN -> 25
        ButtonId.MUTE -> 164
        ButtonId.CH_UP -> 166
        ButtonId.CH_DOWN -> 167
        ButtonId.PLAY -> 126
        ButtonId.PAUSE -> 127
        ButtonId.STOP -> 86
        ButtonId.FFWD -> 90
        ButtonId.RWD -> 89
        ButtonId.INFO -> 165
        ButtonId.GUIDE -> 172
        ButtonId.SOURCE -> 178
        ButtonId.DIGIT_0 -> 7
        ButtonId.DIGIT_1 -> 8
        ButtonId.DIGIT_2 -> 9
        ButtonId.DIGIT_3 -> 10
        ButtonId.DIGIT_4 -> 11
        ButtonId.DIGIT_5 -> 12
        ButtonId.DIGIT_6 -> 13
        ButtonId.DIGIT_7 -> 14
        ButtonId.DIGIT_8 -> 15
        ButtonId.DIGIT_9 -> 16
        else -> null
    }
}
