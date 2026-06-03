package com.gnutux.tahakom.core.transport.impl

import com.gnutux.tahakom.core.irdb.Pronto
import com.gnutux.tahakom.core.model.Command
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.transport.Transport
import com.gnutux.tahakom.core.transport.TransportError
import com.gnutux.tahakom.core.transport.TransportResult
import com.gnutux.tahakom.core.transport.TransportType
import com.gnutux.tahakom.core.transport.impl.broadlink.Broadlink
import com.gnutux.tahakom.core.transport.impl.broadlink.BroadlinkIr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * **تجريبي — تحت التطوير.** جسر WiFi-IR (Broadlink): يحوّل كود Pronto إلى حزمة Broadlink
 * ويرسلها عبر WiFi فيبثّها الجسر كأشعة تحت حمراء — فيتحكّم بأجهزة IR من **أي هاتف**
 * (حتى بلا باعث مدمج). يحتاج اكتشاف الجسر + مصادقته. أكواد IR تأتي من قاعدة IR نفسها
 * (يُمرّر [Command.Raw] بكود Pronto، تُحلّه الطبقة الأعلى من irFile).
 */
class BroadlinkTransport : Transport {

    override val type = TransportType.BROADLINK

    // جلسة مُصادَقة لكل مضيف (تُعاد بناؤها عند الحاجة).
    @Volatile private var session: Broadlink.Session? = null

    override suspend fun isAvailable(): Boolean = true

    override suspend fun connect(device: Device): TransportResult<Unit> = withContext(Dispatchers.IO) {
        val host = device.address
        val found = Broadlink.discover(host) // host=null → أول جسر؛ وإلا المطابق للـ IP
            ?: return@withContext TransportResult.Failure(TransportError.NOT_CONNECTED)
        val s = Broadlink.authenticate(found)
            ?: return@withContext TransportResult.Failure(TransportError.PAIRING_FAILED)
        session = s
        TransportResult.Success(Unit)
    }

    override suspend fun send(device: Device, command: Command): TransportResult<Unit> = withContext(Dispatchers.IO) {
        // أكواد IR تصل كـ Pronto (Command.Raw) أو IrSignal — نحوّلها لحزمة Broadlink.
        val signal = when (command) {
            is Command.Raw -> Pronto.parse(command.payload)
            is Command.IrSignal -> Pronto.Signal(command.frequencyHz, command.pattern)
            else -> null
        } ?: return@withContext TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)

        var s = session
        if (s == null) {
            when (val c = connect(device)) { is TransportResult.Failure -> return@withContext c; else -> {} }
            s = session ?: return@withContext TransportResult.Failure(TransportError.NOT_CONNECTED)
        }
        val packet = BroadlinkIr.fromProntoPattern(signal.pattern)
        if (Broadlink.sendIr(s, packet)) TransportResult.Success(Unit)
        else TransportResult.Failure(TransportError.NETWORK)
    }

    override suspend fun disconnect(device: Device) { session = null }
}
