package com.gnutux.tahakom.core.transport

import com.gnutux.tahakom.core.model.Command
import com.gnutux.tahakom.core.model.Device

/**
 * طبقة النقل المجرّدة — **جوهر معمارية GT-TAHAKOM**.
 *
 * كل بروتوكول (IR، Android TV، Roku، Samsung، LG، Sony، Broadlink) يطبّق هذه الواجهة.
 * هذا تعميمٌ لفكرة `Transmitter.getInstance()` في IRRemote: بدلاً من وسيلة واحدة
 * (الباعث المدمج)، يختار [TransportRegistry] الوسيلة المناسبة لكل جهاز.
 *
 * كل الدوال `suspend` لأن معظم الوسائل شبكية (WebSocket/HTTP/سوكِت).
 */
interface Transport {

    val type: TransportType

    /** هل هذه الوسيلة متاحة الآن؟ (مثلاً: هل يحتوي الهاتف على باعث IR؟). */
    suspend fun isAvailable(): Boolean

    /**
     * إقران/اتصال بالجهاز إن لزم (إقران TLS لـ Android TV، token لـ Samsung...).
     * الوسائل عديمة الاتصال (مثل IR) تُرجع [TransportResult.Success] مباشرة.
     */
    suspend fun connect(device: Device): TransportResult<Unit>

    /** إرسال أمر تحكّم واحد إلى الجهاز. */
    suspend fun send(device: Device, command: Command): TransportResult<Unit>

    /** قطع الاتصال وتحرير الموارد. */
    suspend fun disconnect(device: Device)
}
