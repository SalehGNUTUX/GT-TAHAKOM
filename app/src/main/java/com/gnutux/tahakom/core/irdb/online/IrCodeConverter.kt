package com.gnutux.tahakom.core.irdb.online

import com.gnutux.tahakom.core.irdb.IrButton
import kotlin.math.roundToInt

/**
 * يحوّل صفوف CSV من probonopd/irdb (functionname,protocol,device,subdevice,function)
 * إلى أزرار [IrButton] بأكواد Pronto hex — **على الهاتف** عند الجلب الشبكي.
 *
 * منقول حرفياً من `tools/import_irdb.py` (NEC/NECx + RC5/RC5X + RC6 + تطبيع الأسماء).
 * أي تعديل هنا يجب أن يبقى مطابقاً للسكربت لضمان أكواد متطابقة بين البناء والجلب الحيّ.
 */
object IrCodeConverter {

    private const val NEC_FREQ = 38000
    private const val RC_FREQ = 36000
    private const val SIRC_FREQ = 40000
    private const val PANA_FREQ = 37000
    private const val MITS_FREQ = 32600

    /** بروتوكولات مدعومة (يطابق online_index.json supported flag وأداة build_online_index.py). */
    fun isSupported(protocol: String): Boolean {
        val p = protocol.uppercase()
        return p.startsWith("NEC") || p == "RC5" || p == "RC5X" || p == "RC6" ||
            p.startsWith("SONY") || p == "SIRC" || p == "PANASONIC" ||
            p == "JVC" || p == "MITSUBISHI" || p == "DENON"
    }

    /** يحوّل نص CSV كامل إلى قائمة أزرار (يتخطّى البروتوكولات غير المدعومة والصفوف التالفة). */
    fun convert(csvText: String): List<IrButton> {
        val lines = csvText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()
        val out = ArrayList<IrButton>()
        // أول سطر ترويسة — نتخطّاه.
        for (line in lines.drop(1)) {
            val cols = line.split(",")
            if (cols.size < 5) continue
            val name = cols[0].trim().uppercase()
            val proto = cols[1].trim()
            val dev = cols[2].trim().toIntOrNull() ?: continue
            val sub = cols[3].trim().toIntOrNull() ?: continue
            val fn = cols[4].trim().toIntOrNull() ?: continue
            val code = encode(proto, dev, sub, fn) ?: continue
            out.add(IrButton(id = normalizeButton(name), code = code, freq = freqFor(proto.uppercase()), label = name))
        }
        return out
    }

    private fun freqFor(p: String): Int = when {
        p.startsWith("RC") -> RC_FREQ
        p.startsWith("SONY") || p == "SIRC" -> SIRC_FREQ
        p == "PANASONIC" -> PANA_FREQ
        p == "MITSUBISHI" -> MITS_FREQ
        else -> NEC_FREQ // NEC/JVC/Denon = 38kHz
    }

    private fun encode(protocol: String, device: Int, subdevice: Int, function: Int): String? {
        val p = protocol.uppercase()
        return when {
            p.startsWith("NEC") -> necToPronto(device, subdevice, function)
            p == "RC5" || p == "RC5X" -> rc5ToPronto(device, function)
            p == "RC6" -> rc6ToPronto(device, function)
            p.startsWith("SONY") || p == "SIRC" -> {
                val nbits = if (p.endsWith("15")) 15 else if (p.endsWith("20")) 20 else 12
                sonyToPronto(nbits, device, subdevice, function)
            }
            p == "PANASONIC" -> panasonicToPronto(device, subdevice, function)
            p == "JVC" -> jvcToPronto(device, function)
            p == "MITSUBISHI" -> mitsubishiToPronto(device, function)
            p == "DENON" -> denonToPronto(device, function)
            else -> null
        }
    }

    private fun jvcToPronto(device: Int, function: Int): String {
        val durs = ArrayList<Int>()
        durs.add(8400); durs.add(4200) // قائد 16/8 وحدة
        for (b in intArrayOf(device and 0xFF, function and 0xFF)) {
            for (i in 0 until 8) {
                durs.add(525); durs.add(if ((b shr i) and 1 == 1) 1575 else 525)
            }
        }
        durs.add(525); durs.add(23625) // توقّف + فاصل
        return durationsToPronto(38000, durs)
    }

    private fun mitsubishiToPronto(device: Int, function: Int): String {
        val durs = ArrayList<Int>()
        for (b in intArrayOf(device and 0xFF, function and 0xFF)) {
            for (i in 0 until 8) {
                durs.add(300); durs.add(if ((b shr i) and 1 == 1) 2100 else 900)
            }
        }
        durs.add(300); durs.add(24000)
        return durationsToPronto(MITS_FREQ, durs)
    }

    private fun denonToPronto(device: Int, function: Int): String {
        fun bits(value: Int, n: Int): List<Int> = (0 until n).map { (value shr it) and 1 }
        fun frame(seq: List<Int>): List<Int> {
            val d = ArrayList<Int>()
            for (bit in seq) { d.add(264); d.add(if (bit == 1) 1848 else 792) }
            d.add(264); d.add(43560)
            return d
        }
        val f1 = bits(device, 5) + bits(function and 0xFF, 8) + bits(0, 2)
        val f2 = bits(device, 5) + bits(function.inv() and 0xFF, 8) + bits(3, 2)
        return durationsToPronto(38000, frame(f1) + frame(f2))
    }

    private fun sonyToPronto(nbits: Int, device: Int, subdevice: Int, function: Int): String {
        val value = when (nbits) {
            15 -> (function and 0x7F) or ((device and 0xFF) shl 7)
            20 -> (function and 0x7F) or ((device and 0x1F) shl 7) or ((subdevice and 0xFF) shl 12)
            else -> (function and 0x7F) or ((device and 0x1F) shl 7)
        }
        val durs = ArrayList<Int>()
        durs.add(2400); durs.add(600) // قائد: 4 وحدات ON + وحدة OFF
        for (i in 0 until nbits) {
            durs.add(if ((value shr i) and 1 == 1) 1200 else 600); durs.add(600)
        }
        // مدّ الفضاء الأخير لإكمال الإطار 45ms.
        val burst = durs.dropLast(1).sum()
        durs[durs.size - 1] = maxOf(600, 45000 - burst)
        return durationsToPronto(SIRC_FREQ, durs)
    }

    private fun panasonicToPronto(device: Int, subdevice: Int, function: Int): String {
        val chk = (device xor subdevice xor function) and 0xFF
        val data = intArrayOf(0x02, 0x20, device and 0xFF, subdevice and 0xFF, function and 0xFF, chk)
        val durs = ArrayList<Int>()
        durs.add(3456); durs.add(1728) // قائد: 8 وحدات ON + 4 OFF
        for (b in data) {
            for (i in 0 until 8) {
                durs.add(432); durs.add(if ((b shr i) and 1 == 1) 1296 else 432)
            }
        }
        durs.add(432); durs.add(74736) // ذيل
        return durationsToPronto(PANA_FREQ, durs)
    }

    private fun necToPronto(device: Int, subdevice: Int, function: Int): String {
        val carrier = (1_000_000.0 / (NEC_FREQ * 0.241246)).roundToInt()
        val sub = if (subdevice < 0) device.inv() and 0xFF else subdevice and 0xFF
        val bytes = intArrayOf(device and 0xFF, sub, function and 0xFF, function.inv() and 0xFF)
        val unit = (562.5 / (carrier * 0.241246)).roundToInt()
        val pairs = ArrayList<Int>()
        pairs.add(16 * unit); pairs.add(8 * unit) // رأس 9000us on + 4500us off
        for (b in bytes) {
            for (i in 0 until 8) {
                if ((b shr i) and 1 == 1) { pairs.add(unit); pairs.add(3 * unit) }
                else { pairs.add(unit); pairs.add(unit) }
            }
        }
        pairs.add(unit); pairs.add(39 * unit) // نبضة نهاية
        val n = pairs.size / 2
        val words = ArrayList<Int>(); words.add(0); words.add(carrier); words.add(0); words.add(n); words.addAll(pairs)
        return words.joinToString(" ") { it.toString(16).padStart(4, '0') }
    }

    private fun rc5ToPronto(device: Int, function: Int): String {
        val s2 = if (function > 63) 0 else 1
        val bits = ArrayList<Int>()
        bits.add(1); bits.add(s2); bits.add(0) // S1, S2, toggle
        for (i in 4 downTo 0) bits.add((device shr i) and 1)
        for (i in 5 downTo 0) bits.add((function shr i) and 1)
        val durs = manchesterDurations(bits, 889, rc6 = false, toggleIndex = null)
        return durationsToPronto(RC_FREQ, durs)
    }

    private fun rc6ToPronto(device: Int, function: Int): String {
        val t = 444
        val lead = intArrayOf(6 * t, 2 * t)
        val bits = ArrayList<Int>()
        bits.add(1); bits.add(0); bits.add(0); bits.add(0) // بت بدء + وضع 0
        val toggleIdx = bits.size
        bits.add(0) // بت تبديل (مزدوج العرض)
        for (i in 7 downTo 0) bits.add((device shr i) and 1)
        for (i in 7 downTo 0) bits.add((function shr i) and 1)
        val man = manchesterDurations(bits, t, rc6 = true, toggleIndex = toggleIdx)
        return durationsToPronto(RC_FREQ, lead.toList() + man)
    }

    /** يبني مدد إشارة من بتّات مانشستر (RC5/RC6)، تبدأ بـ ON (تُسقَط أي بادئة OFF). */
    private fun manchesterDurations(bits: List<Int>, halfUs: Int, rc6: Boolean, toggleIndex: Int?): List<Int> {
        val levels = ArrayList<Int>()
        for (i in bits.indices) {
            val b = bits[i]
            val rep = if (toggleIndex != null && i == toggleIndex) 2 else 1
            val first: Int; val second: Int
            if (rc6) { first = if (b == 1) 1 else 0; second = if (b == 1) 0 else 1 }
            else { first = if (b == 1) 0 else 1; second = if (b == 1) 1 else 0 }
            repeat(rep) { levels.add(first) }
            repeat(rep) { levels.add(second) }
        }
        // دمج المتتاليات (RLE)
        val runs = ArrayList<Pair<Int, Int>>()
        var cur = levels[0]; var cnt = 1
        for (k in 1 until levels.size) {
            if (levels[k] == cur) cnt++ else { runs.add(cur to cnt); cur = levels[k]; cnt = 1 }
        }
        runs.add(cur to cnt)
        val trimmed = if (runs.isNotEmpty() && runs[0].first == 0) runs.drop(1) else runs
        return trimmed.map { it.second * halfUs }
    }

    /** يحوّل مدداً (ميكروثانية، تبدأ بـ ON وتتناوب) إلى Pronto hex. */
    private fun durationsToPronto(freq: Int, durationsUs: List<Int>): String {
        val carrier = (1_000_000.0 / (freq * 0.241246)).roundToInt()
        val durs = ArrayList(durationsUs)
        if (durs.size % 2 == 1) durs.add((50 * 889)) // فجوة فاصلة
        val words = durs.map { (it.toDouble() * freq / 1_000_000).roundToInt() }
        val n = words.size / 2
        val head = listOf(0, carrier, 0, n)
        return (head + words).joinToString(" ") { it.toString(16).padStart(4, '0') }
    }

    // ----- تطبيع أسماء الأزرار (منقول من NAME_MAP + _normalize_button) -----

    private val NAME_MAP: Map<String, String> = buildMap {
        put("POWER", "POWER"); put("POWER ON", "POWER_ON"); put("POWER OFF", "POWER_OFF")
        put("VOLUME +", "VOL_UP"); put("VOLUME -", "VOL_DOWN"); put("VOL +", "VOL_UP"); put("VOL -", "VOL_DOWN")
        put("MUTE", "MUTE"); put("CHANNEL +", "CH_UP"); put("CHANNEL -", "CH_DOWN"); put("CH +", "CH_UP"); put("CH -", "CH_DOWN")
        put("UP", "NAV_UP"); put("DOWN", "NAV_DOWN"); put("LEFT", "NAV_LEFT"); put("RIGHT", "NAV_RIGHT")
        put("CURSOR UP", "NAV_UP"); put("CURSOR DOWN", "NAV_DOWN"); put("CURSOR LEFT", "NAV_LEFT"); put("CURSOR RIGHT", "NAV_RIGHT")
        put("ARROW UP", "NAV_UP"); put("ARROW DOWN", "NAV_DOWN"); put("ARROW LEFT", "NAV_LEFT"); put("ARROW RIGHT", "NAV_RIGHT")
        put("OK", "NAV_OK"); put("ENTER", "NAV_OK"); put("SELECT", "NAV_OK")
        put("CH LIST", "LIST"); put("CHANNEL LIST", "LIST"); put("C.C.", "CC"); put("CC", "CC")
        put("BACK", "BACK"); put("RETURN", "BACK"); put("EXIT", "EXIT"); put("HOME", "HOME"); put("MENU", "MENU")
        put("INFO", "INFO"); put("DISPLAY", "DISP"); put("GUIDE", "GUIDE"); put("EPG", "GUIDE")
        put("INPUT SOURCE", "SOURCE"); put("INPUT", "SOURCE"); put("SOURCE", "SOURCE"); put("TV/AV", "SOURCE")
        for (d in 0..9) put(d.toString(), "DIGIT_$d")
        put("PLAY", "PLAY"); put("PAUSE", "PAUSE"); put("STOP", "STOP"); put("FAST FORWARD", "FFWD")
        put("REWIND", "RWD"); put("RECORD", "RECORD"); put("SLEEP", "SLEEP"); put("LIST", "LIST")
        put("RED", "RED"); put("GREEN", "GREEN"); put("YELLOW", "YELLOW"); put("BLUE", "BLUE")
        put("SUBTITLE", "CC"); put("TEXT", "TEXT"); put("FAVORITE", "FAV"); put("FAV", "FAV")
        put("POWERON", "POWER_ON"); put("POWEROFF", "POWER_OFF"); put("OFF", "POWER")
        put("POWERTOGGLE", "POWER"); put("POWER2", "POWER")
        put("VOLUMEUP", "VOL_UP"); put("VOLUMEDOWN", "VOL_DOWN")
        put("CHANNELUP", "CH_UP"); put("CHANNELDOWN", "CH_DOWN")
        put("VOL_P", "VOL_UP"); put("VOL_M", "VOL_DOWN"); put("PROG_P", "CH_UP"); put("PROG_M", "CH_DOWN")
        put("CH_P", "CH_UP"); put("CH_M", "CH_DOWN")
        put("VOLUME_UP", "VOL_UP"); put("VOLUME_DOWN", "VOL_DOWN")
        put("CHANNEL_UP", "CH_UP"); put("CHANNEL_DOWN", "CH_DOWN")
        put("FORWARD", "FFWD"); put("FASTFORWARD", "FFWD"); put("NEXT", "FFWD"); put("PREVIOUS", "RWD")
        put("TVGUIDE", "GUIDE"); put("AUX", "SOURCE"); put("TV", "SOURCE"); put("VIDEO", "SOURCE")
        put("PIP", "PIP"); put("TOOLS", "TOOLS"); put("SMART", "SMART"); put("APPS", "SMART")
    }

    /** يطابق اسم زر probonopd مع ButtonId. يتسامح مع بادئة LIRC ‏KEY_‎ والفواصل. */
    private fun normalizeButton(name: String): String {
        NAME_MAP[name]?.let { return it }
        var core = name
        for (pre in listOf("KEY_", "KEY ", "BTN_")) {
            if (core.startsWith(pre)) { core = core.substring(pre.length); break }
        }
        core = core.trim()
        NAME_MAP[core]?.let { return it }
        if (core.length == 1 && core[0].isDigit()) return "DIGIT_$core"
        val alt = core.replace(Regex("[\\s\\-]+"), "_")
        return NAME_MAP[alt] ?: "UNKNOWN"
    }
}
