package com.gnutux.tahakom.core.transport.impl

import android.content.Context
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Command
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.store.WebosKeyStore
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

/**
 * وسيلة نقل LG webOS عبر بروتوكول SSAP (WebSocket على المنفذ 3000).
 *
 * يحلّ ما يعجز عنه IR على التلفاز الذكي: فتح التطبيقات (يوتيوب…)، التنقّل،
 * تشغيل/إيقاف المقاطع، الإعدادات الذكية — عبر أوامر صريحة (ssap://…).
 *
 * **الإقران:** أول اتصال يرسل سجلّ تسجيل؛ يظهر على التلفاز طلب قبول. بعد القبول
 * يردّ التلفاز بـ client-key نحفظه ([WebosKeyStore]) فلا يتكرّر الطلب لاحقاً.
 */
class WebosTransport(context: Context) : Transport {

    override val type = TransportType.LG_WEBOS

    private val keyStore = WebosKeyStore(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket يبقى مفتوحاً
        .build()

    override suspend fun isAvailable(): Boolean = true

    override suspend fun connect(device: Device): TransportResult<Unit> =
        withContext(Dispatchers.IO) {
            val host = device.address
                ?: return@withContext TransportResult.Failure(TransportError.NOT_CONNECTED)
            // محاولة إقران/اتصال (تنجح إن وُجد مفتاح محفوظ، أو يقبل المستخدم على التلفاز).
            runSession(host) { _, _ -> /* مجرّد تحقق من الاتصال */ }
        }

    override suspend fun send(device: Device, command: Command): TransportResult<Unit> =
        withContext(Dispatchers.IO) {
            val host = device.address
                ?: return@withContext TransportResult.Failure(TransportError.NOT_CONNECTED)
            // أزرار التنقّل والوسائط تُرسَل عبر "pointer input socket" (لا ssap URI).
            val pointerName = (command as? Command.Key)?.button?.toPointerButton()
            if (pointerName != null) return@withContext sendPointer(host, pointerName)

            val uri = when (command) {
                is Command.Key -> command.button.toSsap()
                    ?: return@withContext TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)
                is Command.Raw -> command.payload
                else -> return@withContext TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)
            }
            runSession(host) { ws, idGen ->
                ws.send(
                    JSONObject()
                        .put("id", idGen())
                        .put("type", "request")
                        .put("uri", uri)
                        .toString(),
                )
            }
        }

    /**
     * يرسل زر تنقّل عبر pointer input socket في webOS:
     * يُقرن → يطلب socketPath → يفتح سوكِت ثانياً → يرسل `type:button\nname:<NAME>\n\n`.
     * (جلسة لكل ضغطة — مقبول للزمن؛ يُحسّن باتصال دائم لاحقاً.)
     */
    private suspend fun sendPointer(host: String, buttonName: String): TransportResult<Unit> {
        val done = CompletableDeferred<TransportResult<Unit>>()
        var counter = 1
        val savedKey = keyStore.get(host)
        var pointerWs: WebSocket? = null

        val main = client.newWebSocket(
            Request.Builder().url("ws://$host:3000").build(),
            object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, r: Response) = ws.send(registerPayload(savedKey, counter++)).let {}
                override fun onMessage(ws: WebSocket, text: String) {
                    val msg = runCatching { JSONObject(text) }.getOrNull() ?: return
                    when (msg.optString("type")) {
                        "registered" -> {
                            msg.optJSONObject("payload")?.optString("client-key")?.takeIf { it.isNotEmpty() }
                                ?.let { keyStore.put(host, it) }
                            // اطلب مسار pointer socket
                            ws.send(
                                JSONObject().put("id", counter++).put("type", "request")
                                    .put("uri", "ssap://com.webos.service.networkinput/getPointerInputSocket")
                                    .toString(),
                            )
                        }
                        "response" -> {
                            val path = msg.optJSONObject("payload")?.optString("socketPath")
                            if (!path.isNullOrEmpty() && pointerWs == null) {
                                pointerWs = client.newWebSocket(
                                    Request.Builder().url(path).build(),
                                    object : WebSocketListener() {
                                        override fun onOpen(pws: WebSocket, r: Response) {
                                            pws.send("type:button\nname:$buttonName\n\n")
                                            if (!done.isCompleted) done.complete(TransportResult.Success(Unit))
                                        }
                                        override fun onFailure(pws: WebSocket, t: Throwable, r: Response?) {
                                            if (!done.isCompleted) done.complete(TransportResult.Failure(TransportError.NETWORK, t))
                                        }
                                    },
                                )
                            }
                        }
                        "error" -> if (!done.isCompleted)
                            done.complete(TransportResult.Failure(TransportError.PAIRING_REQUIRED))
                    }
                }
                override fun onFailure(ws: WebSocket, t: Throwable, r: Response?) {
                    if (!done.isCompleted) done.complete(TransportResult.Failure(TransportError.NETWORK, t))
                }
            },
        )
        val result = withTimeoutOrNull(if (savedKey == null) 30_000 else 7_000) { done.await() }
            ?: TransportResult.Failure(TransportError.TIMEOUT)
        runCatching { pointerWs?.close(1000, null) }
        runCatching { main.close(1000, null) }
        return result
    }

    override suspend fun disconnect(device: Device) { /* جلسة لكل أمر — لا اتصال دائم بعد */ }

    /**
     * يفتح جلسة WebSocket، يُقرن (مفتاح محفوظ أو طلب جديد)، ثم ينفّذ [afterRegister].
     * يُغلق الجلسة بعد التنفيذ. مبسّط: جلسة لكل أمر (مقبول للزمن، يُحسّن لاحقاً باتصال دائم).
     */
    private suspend fun runSession(
        host: String,
        afterRegister: (WebSocket, () -> Int) -> Unit,
    ): TransportResult<Unit> {
        val done = CompletableDeferred<TransportResult<Unit>>()
        var counter = 1
        val idGen = { counter++ }
        val savedKey = keyStore.get(host)

        val request = Request.Builder().url("ws://$host:3000").build()
        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(registerPayload(savedKey, idGen()))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = runCatching { JSONObject(text) }.getOrNull() ?: return
                when (msg.optString("type")) {
                    "registered" -> {
                        // التقط client-key واحفظه، ثم نفّذ الأمر.
                        msg.optJSONObject("payload")?.optString("client-key")?.takeIf { it.isNotEmpty() }
                            ?.let { keyStore.put(host, it) }
                        afterRegister(webSocket) { idGen() }
                        if (!done.isCompleted) done.complete(TransportResult.Success(Unit))
                    }
                    "response" -> { /* تأكيد أمر */ }
                    "error" -> {
                        if (!done.isCompleted) {
                            val err = msg.optString("error")
                            done.complete(
                                TransportResult.Failure(
                                    if (err.contains("403") || err.contains("denied")) TransportError.PAIRING_REQUIRED
                                    else TransportError.NETWORK,
                                ),
                            )
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!done.isCompleted) done.complete(TransportResult.Failure(TransportError.NETWORK, t))
            }
        })

        // مهلة: الإقران الأول قد يحتاج قبول المستخدم على التلفاز.
        val result = withTimeoutOrNull(if (savedKey == null) 30_000 else 6_000) { done.await() }
            ?: TransportResult.Failure(TransportError.TIMEOUT)
        runCatching { ws.close(1000, null) }
        return result
    }

    /** سجلّ تسجيل SSAP القياسي (manifest أذونات webOS). */
    private fun registerPayload(clientKey: String?, id: Int): String {
        val manifest = JSONObject()
            .put("manifestVersion", 1)
            .put(
                "permissions",
                org.json.JSONArray(
                    listOf(
                        "CONTROL_POWER", "CONTROL_INPUT_MEDIA_PLAYBACK", "CONTROL_INPUT_TV",
                        "CONTROL_INPUT_JOYSTICK", "CONTROL_AUDIO", "LAUNCH", "LAUNCH_WEBAPP",
                        "READ_INSTALLED_APPS", "CONTROL_DISPLAY", "TEST_OPEN", "TEST_PROTECTED",
                        "READ_TV_CURRENT_CHANNEL", "CONTROL_INPUT_TV", "WRITE_NOTIFICATION_TOAST",
                    ),
                ),
            )
        val payload = JSONObject()
            .put("forcePairing", false)
            .put("pairingType", "PROMPT")
            .put("manifest", manifest)
        if (!clientKey.isNullOrEmpty()) payload.put("client-key", clientKey)
        return JSONObject()
            .put("id", id)
            .put("type", "register")
            .put("payload", payload)
            .toString()
    }

    /** أزرار التنقّل/الإدخال عبر pointer socket (أسماء webOS القياسية). */
    private fun ButtonId.toPointerButton(): String? = when (this) {
        ButtonId.NAV_UP -> "UP"
        ButtonId.NAV_DOWN -> "DOWN"
        ButtonId.NAV_LEFT -> "LEFT"
        ButtonId.NAV_RIGHT -> "RIGHT"
        ButtonId.NAV_OK -> "ENTER"
        ButtonId.BACK -> "BACK"
        ButtonId.EXIT -> "EXIT"
        ButtonId.MENU -> "MENU"
        ButtonId.INFO -> "INFO"
        else -> null
    }

    /** يحوّل الزر الدلالي إلى SSAP URI (لغير أزرار التنقّل). */
    private fun ButtonId.toSsap(): String? = when (this) {
        ButtonId.POWER, ButtonId.POWER_OFF -> "ssap://system/turnOff"
        ButtonId.VOL_UP -> "ssap://audio/volumeUp"
        ButtonId.VOL_DOWN -> "ssap://audio/volumeDown"
        ButtonId.MUTE -> "ssap://audio/setMute"
        ButtonId.CH_UP -> "ssap://tv/channelUp"
        ButtonId.CH_DOWN -> "ssap://tv/channelDown"
        ButtonId.HOME -> "ssap://com.webos.applicationManager/launch?id=com.webos.app.home"
        ButtonId.APPS, ButtonId.SMART -> "ssap://com.webos.applicationManager/launch?id=com.webos.app.discovery"
        ButtonId.INFO -> "ssap://com.webos.service.tv.display/getCurrentChannel"
        ButtonId.PLAY -> "ssap://media.controls/play"
        ButtonId.PAUSE -> "ssap://media.controls/pause"
        ButtonId.STOP -> "ssap://media.controls/stop"
        ButtonId.FFWD -> "ssap://media.controls/fastForward"
        ButtonId.RWD -> "ssap://media.controls/rewind"
        ButtonId.LIST -> "ssap://com.webos.applicationManager/launch?id=com.webos.app.channeledit"
        ButtonId.SETTINGS -> "ssap://system.launcher/open?id=com.webos.app.settings"
        // التنقّل (up/down/left/right/ok/back) يحتاج pointer input socket — مرحلة تالية.
        else -> null
    }
}
