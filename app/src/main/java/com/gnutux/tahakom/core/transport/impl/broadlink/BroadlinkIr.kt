package com.gnutux.tahakom.core.transport.impl.broadlink

import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * يحوّل نمط نبضات IR (مدد بالميكروثانية، من Pronto) إلى حزمة Broadlink IR.
 * كل مدّة → «نبضات» Broadlink (1 وحدة ≈ 8192/269 ≈ 30.5μs)؛ القيم ≥256 تُرمَّز بثلاث
 * بايتات (0x00 ثم big-endian). الحزمة: 0x26 (IR) + تكرار + الطول + البيانات + 0x0d 0x05.
 */
object BroadlinkIr {

    fun fromProntoPattern(pattern: IntArray): ByteArray {
        val data = ByteArrayOutputStream()
        for (us in pattern) {
            val ticks = (us * 269.0 / 8192.0).roundToInt()
            if (ticks in 1..255) {
                data.write(ticks)
            } else {
                data.write(0x00)
                data.write((ticks ushr 8) and 0xFF)
                data.write(ticks and 0xFF)
            }
        }
        val len = data.size()
        val out = ByteArrayOutputStream()
        out.write(0x26) // IR
        out.write(0x00) // عدد التكرارات
        out.write(len and 0xFF); out.write((len ushr 8) and 0xFF) // الطول little-endian
        data.writeTo(out)
        out.write(0x0d); out.write(0x05) // نهاية
        return out.toByteArray()
    }
}
