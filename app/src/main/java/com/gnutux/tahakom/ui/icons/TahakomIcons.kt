package com.gnutux.tahakom.ui.icons

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * مجموعة الأيقونات الخطّية المقتبسة حرفياً من GT-TAHAKOM-DESIGN/src/icons.jsx.
 * كلها viewBox 24×24، stroke = currentColor، linecap/linejoin = round، strokeWidth 1.7.
 * تحلّ مشكلة الأيقونات الناقصة (مثل الطاقة) في النسخة السابقة.
 *
 * الأيقونة تُرسَم باللون المعطى عبر [tint] (يُمرَّر من مكان الاستخدام).
 */
object TahakomIcons {

    // مسارات SVG حرفية من icons.jsx (d=…). كل عنصر قائمة = <path d>.
    private val PATHS: Map<String, List<String>> = mapOf(
        "back" to listOf("M15 5l-7 7 7 7"),
        "forwardNav" to listOf("M9 5l7 7-7 7"),
        "chevron" to listOf("M9 6l6 6-6 6"),
        "close" to listOf("M6 6l12 12M18 6L6 18"),
        "plus" to listOf("M12 5v14M5 12h14"),
        "check" to listOf("M5 12.5l4.5 4.5L19 7"),
        "search" to listOf("M20 20l-4-4"),
        "gear" to listOf("M12 2.5v3M12 18.5v3M21.5 12h-3M5.5 12h-3M18.7 5.3l-2.1 2.1M7.4 16.6l-2.1 2.1M18.7 18.7l-2.1-2.1M7.4 7.4L5.3 5.3"),
        "info" to listOf("M12 11v5M12 7.6v.1"),
        "globe" to listOf("M3 12h18M12 3c2.6 2.4 2.6 15.6 0 18M12 3c-2.6 2.4-2.6 15.6 0 18"),
        "sun" to listOf("M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.6 5.6l1.8 1.8M16.6 16.6l1.8 1.8M18.4 5.6l-1.8 1.8M7.4 16.6l-1.8 1.8"),
        "moon" to listOf("M20 14.5A8 8 0 119.5 4a6.5 6.5 0 0010.5 10.5z"),
        "power" to listOf("M12 3.5v8", "M6.6 7A8 8 0 1017.4 7"),
        "homeBtn" to listOf("M4 11.5L12 5l8 6.5M6 10v9h12v-9"),
        "caretUp" to listOf("M7 14l5-5 5 5"),
        "caretDown" to listOf("M7 10l5 5 5-5"),
        "caretLeft" to listOf("M14 7l-5 5 5 5"),
        "caretRight" to listOf("M10 7l5 5-5 5"),
        "volUp" to listOf("M4 9.5v5h3l4.5 3.5v-12L7 9.5H4z", "M16 8.5a5 5 0 010 7M18.5 6a8.5 8.5 0 010 12"),
        "volDown" to listOf("M4 9.5v5h3l4.5 3.5v-12L7 9.5H4z", "M16 9.5a4 4 0 010 5"),
        "mute" to listOf("M4 9.5v5h3l4.5 3.5v-12L7 9.5H4z", "M15.5 9.5l5 5M20.5 9.5l-5 5"),
        "play" to listOf("M7 5l12 7-12 7z"),
        "pause" to listOf("M9 5v14M15 5v14"),
        "rewind" to listOf("M11 6l-7 6 7 6zM20 6l-7 6 7 6z"),
        "forward" to listOf("M13 6l7 6-7 6zM4 6l7 6-7 6z"),
        "mic" to listOf("M5.5 11.5a6.5 6.5 0 0013 0M12 18v3"),
        "source" to listOf("M8 21h8"),
        "menu" to listOf("M4 7h16M4 12h16M4 17h16"),
        "pointer" to listOf("M6 3l13 7.5-5.6 1.4-1.4 5.6z"),
        "guide" to listOf("M3 9h18M9 9v11"),
        "subtitles" to listOf("M7 11h4M7 14.5h7M14 11h3"),
        "info2" to listOf("M12 15v-4M12 8.5v.1"),
        "tv" to listOf("M8 21h8M12 17v4"),
        "ac" to listOf("M6 17v.5M10 18v.5M14 17v.5M18 18v.5"),
        "receiver" to listOf("M11 12h6"),
        "wifi" to listOf("M2.5 9a14 14 0 0119 0M5.5 12.5a9.5 9.5 0 0113 0M8.5 16a5 5 0 017 0"),
        "ir" to listOf("M12 12v.01", "M8.5 8.5a5 5 0 000 7M15.5 8.5a5 5 0 010 7M6 6a9 9 0 000 12M18 6a9 9 0 010 12"),
        "bridge" to listOf("M7 12a5 5 0 015-5M17 12a5 5 0 01-5 5M4 12a8 8 0 018-8M20 12a8 8 0 01-8 8"),
        "link" to listOf("M9 12h6", "M10 8H8a4 4 0 000 8h2M14 8h2a4 4 0 010 8h-2"),
        "signal" to listOf("M4 18v-2M9 18v-5M14 18v-8M19 18V8"),
        "scan" to listOf("M4 8V5.5A1.5 1.5 0 015.5 4H8M16 4h2.5A1.5 1.5 0 0120 5.5V8M20 16v2.5a1.5 1.5 0 01-1.5 1.5H16M8 20H5.5A1.5 1.5 0 014 18.5V16", "M4 12h16"),
        "shield" to listOf("M12 3l7 3v5c0 5-3 7.5-7 9-4-1.5-7-4-7-9V6z"),
        "swap" to listOf("M7 7h11l-3-3M17 17H6l3 3"),
        "more" to listOf("M12 3.6v.1M12 11.9v.1M12 19.4v.1"),
        "delete" to listOf("M5 7h14M9 7V5h6v2M7 7l1 13h8l1-13"),
        "share" to listOf("M16 6l-4-3-4 3M12 3v12M5 12v7h14v-7"),
    )

    /** أسماء الأيقونات المتاحة. */
    val names: Set<String> get() = PATHS.keys

    /**
     * يبني [ImageVector] لأيقونة باسمها، أو أيقونة نقطة احتياطية إن لم توجد.
     * يُمرَّر [tint] لتلوين الحدّ. لا تخزين مسبق — Compose يخزّن النتيجة عبر remember في الاستخدام.
     */
    fun vector(name: String, tint: Color, strokeWidth: Float = 1.7f): ImageVector {
        val paths = PATHS[name] ?: listOf("M12 12m-4 0a4 4 0 108 0a4 4 0 10-8 0") // dot fallback
        return ImageVector.Builder(
            name = "tahakom_$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = parseSvgPath(d),
                    stroke = SolidColor(tint),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
    }
}

/**
 * محلّل بسيط لمسار SVG (d) → أوامر PathBuilder.
 * يدعم: M m L l H h V v C c Z z A a (القوس يُقرّب بخط مستقيم — كافٍ للأيقونات الصغيرة).
 */
internal fun parseSvgPath(d: String): List<androidx.compose.ui.graphics.vector.PathNode> {
    val b = PathBuilder()
    var i = 0
    val n = d.length
    var cmd = ' '

    fun skipSep() {
        while (i < n && (d[i] == ' ' || d[i] == ',' || d[i] == '\t' || d[i] == '\n')) i++
    }
    fun readNum(): Float {
        skipSep()
        val start = i
        if (i < n && (d[i] == '+' || d[i] == '-')) i++
        while (i < n && (d[i].isDigit() || d[i] == '.')) {
            // التعامل مع رقم علمي نادر — نتجاهله؛ الأرقام هنا عشرية بسيطة
            i++
        }
        return d.substring(start, i).toFloat()
    }

    while (i < n) {
        skipSep()
        if (i >= n) break
        val ch = d[i]
        if (ch.isLetter()) {
            cmd = ch
            i++
        }
        when (cmd) {
            'M' -> b.moveTo(readNum(), readNum())
            'm' -> b.moveToRelative(readNum(), readNum())
            'L' -> b.lineTo(readNum(), readNum())
            'l' -> b.lineToRelative(readNum(), readNum())
            'H' -> b.horizontalLineTo(readNum())
            'h' -> b.horizontalLineToRelative(readNum())
            'V' -> b.verticalLineTo(readNum())
            'v' -> b.verticalLineToRelative(readNum())
            'C' -> b.curveTo(readNum(), readNum(), readNum(), readNum(), readNum(), readNum())
            'c' -> b.curveToRelative(readNum(), readNum(), readNum(), readNum(), readNum(), readNum())
            'A' -> {
                val rx = readNum(); val ry = readNum(); val rot = readNum()
                val laf = readNum() != 0f; val swf = readNum() != 0f
                b.arcTo(rx, ry, rot, laf, swf, readNum(), readNum())
            }
            'a' -> {
                val rx = readNum(); val ry = readNum(); val rot = readNum()
                val laf = readNum() != 0f; val swf = readNum() != 0f
                b.arcToRelative(rx, ry, rot, laf, swf, readNum(), readNum())
            }
            'Z', 'z' -> b.close()
            else -> i++ // تخطّي غير المدعوم
        }
    }
    return b.nodes
}
