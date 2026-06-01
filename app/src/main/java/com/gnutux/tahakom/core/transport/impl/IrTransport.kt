package com.gnutux.tahakom.core.transport.impl

import android.content.Context
import android.hardware.ConsumerIrManager
import com.gnutux.tahakom.core.irdb.Pronto
import com.gnutux.tahakom.core.model.Command
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.transport.Transport
import com.gnutux.tahakom.core.transport.TransportError
import com.gnutux.tahakom.core.transport.TransportResult
import com.gnutux.tahakom.core.transport.TransportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * وسيلة نقل الأشعة تحت الحمراء عبر الباعث المدمج في الهاتف.
 *
 * مقتبسة من `KitKatTransmitter` في IRRemote. تستقبل [Command.IrSignal] (نمط جاهز)
 * أو [Command.Raw] (كود Pronto hex من القاعدة المحلية) وتحوّله عبر [Pronto].
 * الباعث **اختياري**: إن لم يوجد تعود [isAvailable] بـ false.
 */
class IrTransport(context: Context) : Transport {

    override val type = TransportType.IR

    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    override suspend fun isAvailable(): Boolean = irManager?.hasIrEmitter() == true

    override suspend fun connect(device: Device): TransportResult<Unit> =
        if (isAvailable()) TransportResult.Success(Unit)
        else TransportResult.Failure(TransportError.NOT_AVAILABLE)

    override suspend fun send(device: Device, command: Command): TransportResult<Unit> =
        withContext(Dispatchers.Default) {
            val mgr = irManager
                ?: return@withContext TransportResult.Failure(TransportError.NOT_AVAILABLE)

            val signal = when (command) {
                is Command.IrSignal -> command.frequencyHz to command.pattern
                is Command.Raw -> Pronto.parse(command.payload)?.let { it.frequencyHz to it.pattern }
                    ?: return@withContext TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)
                // الأزرار الدلالية تُترجَم إلى Raw(Pronto) في طبقة أعلى عبر IrDatabase.
                else -> return@withContext TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)
            }
            try {
                mgr.transmit(signal.first, signal.second)
                TransportResult.Success(Unit)
            } catch (e: Exception) {
                TransportResult.Failure(TransportError.UNKNOWN, e)
            }
        }

    override suspend fun disconnect(device: Device) {
        // IR عديم الاتصال — لا شيء لتحريره.
    }
}
