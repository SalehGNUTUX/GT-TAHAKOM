package com.gnutux.tahakom.core.transport.impl.androidtv

import java.io.ByteArrayOutputStream

/**
 * مُرمِّز/مُحلِّل Protocol Buffers مُصغّر — يكفي رسائل بروتوكول Android TV Remote v2
 * (لا نعتمد مكتبة protobuf كاملة لجزء تجريبي). يدعم: varint، حقول length-delimited،
 * الرسائل المتداخلة، والأعداد. الترميز السلكي القياسي:
 *   tag = (field << 3) | wire ؛ wire: 0=varint، 2=length-delimited.
 *
 * **تجريبي:** أرقام الحقول مأخوذة من المواصفة المفتوحة لبروتوكول Android TV Remote v2؛
 * تحتاج تحقّقاً على جهاز فعلي.
 */
class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun varintField(field: Int, value: Long): ProtoWriter {
        writeTag(field, 0); writeVarint(value); return this
    }

    fun int32Field(field: Int, value: Int): ProtoWriter = varintField(field, value.toLong())

    fun bytesField(field: Int, value: ByteArray): ProtoWriter {
        writeTag(field, 2); writeVarint(value.size.toLong()); out.write(value); return this
    }

    fun stringField(field: Int, value: String): ProtoWriter = bytesField(field, value.toByteArray(Charsets.UTF_8))

    /** حقل رسالة متداخلة (length-delimited). */
    fun messageField(field: Int, sub: ProtoWriter): ProtoWriter = bytesField(field, sub.toByteArray())

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun writeTag(field: Int, wire: Int) = writeVarint(((field shl 3) or wire).toLong())

    private fun writeVarint(v: Long) {
        var value = v
        while (true) {
            val b = (value and 0x7F).toInt()
            value = value ushr 7
            if (value != 0L) out.write(b or 0x80) else { out.write(b); break }
        }
    }
}

/** قارئ بسيط: يُرجع خريطة رقم الحقل → قائمة قيمه (Long لـ varint، ByteArray لـ length-delimited). */
object ProtoReader {
    fun parse(data: ByteArray): Map<Int, MutableList<Any>> {
        val map = HashMap<Int, MutableList<Any>>()
        var i = 0
        while (i < data.size) {
            val (tag, t1) = readVarint(data, i); i = t1
            val field = (tag ushr 3).toInt()
            when ((tag and 0x7).toInt()) {
                0 -> { val (v, n) = readVarint(data, i); i = n; map.getOrPut(field) { mutableListOf() }.add(v) }
                2 -> {
                    val (len, n) = readVarint(data, i); i = n
                    val end = (i + len.toInt()).coerceAtMost(data.size)
                    map.getOrPut(field) { mutableListOf() }.add(data.copyOfRange(i, end)); i = end
                }
                5 -> i += 4   // fixed32 (غير مستخدم)
                1 -> i += 8   // fixed64 (غير مستخدم)
                else -> return map // نوع غير معروف — توقّف بأمان
            }
        }
        return map
    }

    private fun readVarint(data: ByteArray, start: Int): Pair<Long, Int> {
        var result = 0L; var shift = 0; var i = start
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF; i++
            result = result or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        return result to i
    }
}
