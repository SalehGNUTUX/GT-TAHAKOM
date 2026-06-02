package com.gnutux.tahakom.core.transport.impl

import android.content.Context
import android.util.Base64
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Command
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.store.SimpleTokenStore
import com.gnutux.tahakom.core.transport.Transport
import com.gnutux.tahakom.core.transport.TransportError
import com.gnutux.tahakom.core.transport.TransportResult
import com.gnutux.tahakom.core.transport.TransportType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * وسيلة نقل تلفازات Samsung الذكية (Tizen) عبر WebSocket.
 *
 * المنفذ 8002 (wss آمن) — التلفاز يطلب قبول الاتصال أول مرة ويردّ token يُحفظ
 * ([SimpleTokenStore]). الأوامر مفاتيح Tizen (KEY_POWER, KEY_VOLUP...) ترسَل
 * كـ ms.remote.control. شهادة التلفاز ذاتية التوقيع لذا نتساهل في TLS داخل الشبكة المحلية.
 */
class SamsungTizenTransport(context: Context) : Transport {

    override val type = TransportType.SAMSUNG_TIZEN

    private val tokenStore = SimpleTokenStore(context, "samsung_tokens")
    // اسم التطبيق المعروض على التلفاز عند الإقران (Base64).
    private val appNameB64 = Base64.encodeToString("GT-TAHAKOM".toByteArray(), Base64.NO_WRAP)

    private val client: OkHttpClient = buildInsecureClient()

    override suspend fun isAvailable(): Boolean = true

    override suspend fun connect(device: Device): TransportResult<Unit> =
        withContext(Dispatchers.IO) {
            val host = device.address ?: return@withContext TransportResult.Failure(TransportError.NOT_CONNECTED)
            runSession(host) { /* مجرّد تحقق/إقران */ }
        }

    override suspend fun send(device: Device, command: Command): TransportResult<Unit> =
        withContext(Dispatchers.IO) {
            val host = device.address ?: return@withContext TransportResult.Failure(TransportError.NOT_CONNECTED)
            val key = when (command) {
                is Command.Key -> command.button.toTizenKey()
                    ?: return@withContext TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)
                is Command.Raw -> command.payload
                else -> return@withContext TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)
            }
            runSession(host) { ws ->
                ws.send(
                    JSONObject()
                        .put("method", "ms.remote.control")
                        .put(
                            "params",
                            JSONObject()
                                .put("Cmd", "Click")
                                .put("DataOfCmd", key)
                                .put("Option", "false")
                                .put("TypeOfRemote", "SendRemoteKey"),
                        )
                        .toString(),
                )
            }
        }

    override suspend fun disconnect(device: Device) {}

    private suspend fun runSession(host: String, afterConnect: (WebSocket) -> Unit): TransportResult<Unit> {
        val done = CompletableDeferred<TransportResult<Unit>>()
        val savedToken = tokenStore.get(host)
        val tokenParam = savedToken?.let { "&token=$it" } ?: ""
        val url = "wss://$host:8002/api/v2/channels/samsung.remote.control?name=$appNameB64$tokenParam"

        val request = Request.Builder().url(url).build()
        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = runCatching { JSONObject(text) }.getOrNull() ?: return
                when (msg.optString("event")) {
                    "ms.channel.connect" -> {
                        // التقط token الجديد إن وُجد واحفظه.
                        msg.optJSONObject("data")?.optString("token")?.takeIf { it.isNotEmpty() }
                            ?.let { tokenStore.put(host, it) }
                        afterConnect(webSocket)
                        if (!done.isCompleted) done.complete(TransportResult.Success(Unit))
                    }
                    "ms.channel.unauthorized" ->
                        if (!done.isCompleted) done.complete(TransportResult.Failure(TransportError.PAIRING_REQUIRED))
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!done.isCompleted) done.complete(TransportResult.Failure(TransportError.NETWORK, t))
            }
        })
        val result = withTimeoutOrNull(if (savedToken == null) 30_000 else 6_000) { done.await() }
            ?: TransportResult.Failure(TransportError.TIMEOUT)
        runCatching { ws.close(1000, null) }
        return result
    }

    /** مفاتيح Tizen القياسية. */
    private fun ButtonId.toTizenKey(): String? = when (this) {
        ButtonId.POWER -> "KEY_POWER"
        ButtonId.VOL_UP -> "KEY_VOLUP"
        ButtonId.VOL_DOWN -> "KEY_VOLDOWN"
        ButtonId.MUTE -> "KEY_MUTE"
        ButtonId.CH_UP -> "KEY_CHUP"
        ButtonId.CH_DOWN -> "KEY_CHDOWN"
        ButtonId.NAV_UP -> "KEY_UP"
        ButtonId.NAV_DOWN -> "KEY_DOWN"
        ButtonId.NAV_LEFT -> "KEY_LEFT"
        ButtonId.NAV_RIGHT -> "KEY_RIGHT"
        ButtonId.NAV_OK -> "KEY_ENTER"
        ButtonId.BACK -> "KEY_RETURN"
        ButtonId.HOME -> "KEY_HOME"
        ButtonId.MENU -> "KEY_MENU"
        ButtonId.EXIT -> "KEY_EXIT"
        ButtonId.INFO -> "KEY_INFO"
        ButtonId.GUIDE -> "KEY_GUIDE"
        ButtonId.TOOLS -> "KEY_TOOLS"
        ButtonId.SOURCE -> "KEY_SOURCE"
        ButtonId.SMART, ButtonId.APPS -> "KEY_HOME"
        ButtonId.LIST -> "KEY_CH_LIST"
        ButtonId.PLAY -> "KEY_PLAY"
        ButtonId.PAUSE -> "KEY_PAUSE"
        ButtonId.STOP -> "KEY_STOP"
        ButtonId.FFWD -> "KEY_FF"
        ButtonId.RWD -> "KEY_REWIND"
        ButtonId.DIGIT_0 -> "KEY_0"
        ButtonId.DIGIT_1 -> "KEY_1"
        ButtonId.DIGIT_2 -> "KEY_2"
        ButtonId.DIGIT_3 -> "KEY_3"
        ButtonId.DIGIT_4 -> "KEY_4"
        ButtonId.DIGIT_5 -> "KEY_5"
        ButtonId.DIGIT_6 -> "KEY_6"
        ButtonId.DIGIT_7 -> "KEY_7"
        ButtonId.DIGIT_8 -> "KEY_8"
        ButtonId.DIGIT_9 -> "KEY_9"
        ButtonId.RED -> "KEY_RED"
        ButtonId.GREEN -> "KEY_GREEN"
        ButtonId.YELLOW -> "KEY_YELLOW"
        ButtonId.BLUE -> "KEY_BLUE"
        else -> null
    }

    /** عميل OkHttp يتساهل في شهادة TLS ذاتية التوقيع (داخل الشبكة المحلية فقط). */
    private fun buildInsecureClient(): OkHttpClient {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        }
        val ssl = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustAll), java.security.SecureRandom())
        }
        return OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, trustAll)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }
}
