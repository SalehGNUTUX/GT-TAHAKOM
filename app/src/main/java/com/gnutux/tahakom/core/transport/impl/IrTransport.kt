package com.gnutux.tahakom.core.transport.impl

import android.content.Context
import android.hardware.ConsumerIrManager
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
 * مقتبسة من `KitKatTransmitter` في IRRemote (تستدعي `ConsumerIrManager.transmit`).
 * بخلاف IRRemote، الباعث هنا **اختياري**: إن لم يوجد تعود [isAvailable] بـ false
 * ويتولّى المستخدم التحكم عبر وسائل أخرى (شبكة / Broadlink).
 *
 * TODO(م4): تصحيح الإشارة (SignalCorrector)، الإرسال المتكرر، والتحقق من نطاق التردد.
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

            when (command) {
                is Command.IrSignal -> try {
                    mgr.transmit(command.frequencyHz, command.pattern)
                    TransportResult.Success(Unit)
                } catch (e: Exception) {
                    TransportResult.Failure(TransportError.UNKNOWN, e)
                }
                // الأزرار الدلالية تتطلّب قاعدة أكواد IR (تُضاف في م4).
                else -> TransportResult.Failure(TransportError.UNSUPPORTED_COMMAND)
            }
        }

    override suspend fun disconnect(device: Device) {
        // IR عديم الاتصال — لا شيء لتحريره.
    }
}
