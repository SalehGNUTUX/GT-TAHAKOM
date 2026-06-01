package com.gnutux.tahakom.core.transport.impl

import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Command
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.transport.Transport
import com.gnutux.tahakom.core.transport.TransportError
import com.gnutux.tahakom.core.transport.TransportResult
import com.gnutux.tahakom.core.transport.TransportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * وسيلة نقل Roku عبر بروتوكول ECP (External Control Protocol) — HTTP بسيط.
 *
 * أبسط بروتوكول شبكي: **بلا إقران ولا مصادقة**. أمر = POST إلى
 * `http://<ip>:8060/keypress/<Key>`. يعمل فوراً على أي جهاز Roku على الشبكة.
 * مرجع: https://developer.roku.com/docs/developer-program/dev-tools/external-control-api.md
 */
class RokuTransport : Transport {

    override val type = TransportType.ROKU

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    override suspend fun isAvailable(): Boolean = true // متاح متى توفّرت الشبكة

    override suspend fun connect(device: Device): TransportResult<Unit> =
        TransportResult.Success(Unit) // ECP عديم الاتصال

    override suspend fun send(device: Device, command: Command): TransportResult<Unit> =
        withContext(Dispatchers.IO) {
            val host = device.address
                ?: return@withContext TransportResult.Failure(TransportError.NOT_CONNECTED)
            val port = if (device.transport.defaultPort > 0) device.transport.defaultPort else 8060

            val key = when (command) {
                is Command.Key -> command.button.toRokuKey()
                    ?: return@withContext TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)
                is Command.Text -> "Lit_" + command.value // إدخال نص حرفاً حرفاً يحتاج تكراراً؛ مبسّط هنا
                is Command.Raw -> command.payload
                else -> return@withContext TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)
            }

            val url = "http://$host:$port/keypress/$key"
            val request = Request.Builder().url(url).post(ByteArray(0).toRequestBody()).build()
            try {
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) TransportResult.Success(Unit)
                    else TransportResult.Failure(TransportError.NETWORK)
                }
            } catch (e: Exception) {
                TransportResult.Failure(TransportError.NETWORK, e)
            }
        }

    override suspend fun disconnect(device: Device) { /* لا اتصال دائم */ }

    /** يحوّل الزر الدلالي إلى مفتاح Roku ECP. */
    private fun ButtonId.toRokuKey(): String? = when (this) {
        ButtonId.POWER -> "Power"
        ButtonId.POWER_ON -> "PowerOn"
        ButtonId.POWER_OFF -> "PowerOff"
        ButtonId.NAV_UP -> "Up"
        ButtonId.NAV_DOWN -> "Down"
        ButtonId.NAV_LEFT -> "Left"
        ButtonId.NAV_RIGHT -> "Right"
        ButtonId.NAV_OK -> "Select"
        ButtonId.BACK -> "Back"
        ButtonId.HOME -> "Home"
        ButtonId.INFO -> "Info"
        ButtonId.VOL_UP -> "VolumeUp"
        ButtonId.VOL_DOWN -> "VolumeDown"
        ButtonId.MUTE -> "VolumeMute"
        ButtonId.PLAY, ButtonId.PAUSE -> "Play"
        ButtonId.FFWD -> "Fwd"
        ButtonId.RWD -> "Rev"
        else -> null
    }
}
