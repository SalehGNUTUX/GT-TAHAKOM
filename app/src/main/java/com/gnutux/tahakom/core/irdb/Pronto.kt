package com.gnutux.tahakom.core.irdb

/**
 * يحوّل كود Pronto hex إلى (تردد + نمط نبضات بالميكروثانية) جاهز لـ ConsumerIrManager.
 *
 * مقتبس من `SignalFactory.fromPronto` في IRRemote. بنية Pronto:
 * `word0 word1 word2 word3 [أزواج on/off...]` — كل كلمة hex من 4 خانات.
 * - word0 = 0000 (وضع خام/learned).
 * - word1 = رمز التردد. التردد(Hz) = 1_000_000 / (word1 × 0.241246).
 * - النمط يبدأ من الكلمة الرابعة، وكل قيمة = عدد دورات الحامل تُضرب في طول الدورة.
 */
object Pronto {

    private const val CLOCK = 0.241246 // ميكروثانية لكل وحدة Pronto

    data class Signal(val frequencyHz: Int, val pattern: IntArray)

    /** يحلّل كود Pronto إلى إشارة، أو null إن كان غير صالح. */
    fun parse(code: String): Signal? {
        val words = code.trim().split(Regex("\\s+")).mapNotNull {
            it.toIntOrNull(16)
        }
        if (words.size < 5 || words[0] != 0x0000) return null

        val word1 = words[1]
        if (word1 == 0) return null
        val frequency = (1_000_000.0 / (word1 * CLOCK)).toInt()

        // مدة الدورة الواحدة بالميكروثانية = (word1 × CLOCK).
        val cycleUs = word1 * CLOCK
        val pattern = IntArray(words.size - 4) { i ->
            (words[i + 4] * cycleUs).toInt()
        }
        if (pattern.isEmpty()) return null
        return Signal(frequency, pattern)
    }
}
