package com.gnutux.tahakom.core.transport

import com.gnutux.tahakom.core.model.Device

/**
 * سجلّ وسائل النقل: يختار الـ [Transport] المناسب لجهاز أو نوع معيّن.
 *
 * هذا هو المكافئ الموسّع لـ `Transmitter.getInstance()` في IRRemote —
 * لكنه يدير عدة وسائل بدل واحدة، ويُحقن عبر Hilt (انظر `di/AppModule.kt`).
 */
class TransportRegistry(
    private val transports: List<Transport>,
) {
    /** كل الوسائل المسجّلة. */
    val all: List<Transport> get() = transports

    /** الوسيلة المطابقة لنوع معيّن، أو null إن لم تُسجّل. */
    fun forType(type: TransportType): Transport? =
        transports.firstOrNull { it.type == type }

    /** الوسيلة المناسبة لجهاز معيّن. */
    fun forDevice(device: Device): Transport? =
        forType(device.transport)

    /** الوسائل المتاحة فعلياً على هذا الجهاز/البيئة الآن. */
    suspend fun availableTransports(): List<Transport> =
        transports.filter { it.isAvailable() }
}
