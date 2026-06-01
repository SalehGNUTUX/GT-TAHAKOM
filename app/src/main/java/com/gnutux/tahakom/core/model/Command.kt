package com.gnutux.tahakom.core.model

/**
 * أمر تحكّم مجرّد لا يرتبط بوسيلة نقل بعينها.
 *
 * هذا هو العقد الموحّد بين الواجهة وطبقة النقل: ترسل الواجهة [Command]،
 * ويتولّى كل [com.gnutux.tahakom.core.transport.Transport] ترجمته.
 */
sealed interface Command {

    /** زر دلالي معروف — الطريقة المفضّلة (يعمل عبر كل وسائل النقل). */
    data class Key(val button: ButtonId) : Command

    /** نص حر يُرسل كإدخال (بحث، اسم مستخدم، كلمة مرور...). */
    data class Text(val value: String) : Command

    /**
     * إشارة IR خام: التردد بالهرتز + نمط النبضات (مدد التشغيل/الإطفاء بالميكروثانية).
     * مقتبسة من `Signal` في IRRemote — يستخدمها [TransportType.IR] و[TransportType.BROADLINK].
     */
    data class IrSignal(val frequencyHz: Int, val pattern: IntArray) : Command {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is IrSignal) return false
            return frequencyHz == other.frequencyHz && pattern.contentEquals(other.pattern)
        }

        override fun hashCode(): Int = 31 * frequencyHz + pattern.contentHashCode()
    }

    /** أمر خام خاص بوسيلة نقل معيّنة (مفتاح/مسار/حمولة جاهزة). */
    data class Raw(val payload: String) : Command
}
