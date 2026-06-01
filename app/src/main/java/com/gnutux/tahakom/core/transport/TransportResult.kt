package com.gnutux.tahakom.core.transport

/**
 * نتيجة عملية نقل — بديل صريح عن الاستثناءات لتدفّق أوضح في الواجهة.
 */
sealed interface TransportResult<out T> {
    data class Success<T>(val value: T) : TransportResult<T>
    data class Failure(
        val error: TransportError,
        val cause: Throwable? = null,
    ) : TransportResult<Nothing>
}

/** أصناف أخطاء النقل المعروفة. */
enum class TransportError {
    /** الوسيلة غير متاحة على هذا الجهاز/البيئة (مثلاً لا يوجد باعث IR). */
    NOT_AVAILABLE,
    NOT_CONNECTED,
    PAIRING_REQUIRED,
    PAIRING_FAILED,
    /** الأمر غير مدعوم على وسيلة النقل هذه. */
    UNSUPPORTED_COMMAND,
    NETWORK,
    TIMEOUT,
    UNKNOWN,
}
