package com.gnutux.tahakom.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * نظام رموز التصميم (Design Tokens) المقتبس من GT-TAHAKOM-DESIGN (سمة serene).
 * الألوان محوّلة من OKLCH إلى sRGB. Material 3 ColorScheme لا يغطّي كل الرموز
 * (line/textFaint/surface2/ألوان الوسائل) فنحملها عبر [LocalTokens].
 */
data class TahakomColors(
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val textDim: Color,
    val textFaint: Color,
    val line: Color,
    val lineStrong: Color,
    val accent: Color,
    val accentText: Color,
    val accentSoft: Color,
    // ألوان وسائل النقل (ثابتة عبر السمات، تتغيّر مع الداكن/الفاتح)
    val wifi: Color,
    val ir: Color,
    val bridge: Color,
    val wifiSoft: Color,
    val irSoft: Color,
    val bridgeSoft: Color,
)

data class TahakomShape(
    val sm: Dp = 12.dp,
    val md: Dp = 18.dp,
    val lg: Dp = 26.dp,
    val space: Dp = 18.dp,
    val cardPad: Dp = 18.dp,
)

data class TahakomTokens(
    val colors: TahakomColors,
    val shape: TahakomShape = TahakomShape(),
    val dark: Boolean,
)

// سمة serene الداكنة (محوّلة من OKLCH).
val SereneDark = TahakomColors(
    bg = Color(0xFF0F1318),
    bg2 = Color(0xFF141A20),
    surface = Color(0xFF1B2128),
    surface2 = Color(0xFF232B33),
    text = Color(0xFFEDF3F7),
    textDim = Color(0xFFABB2B8),
    textFaint = Color(0xFF747C81),
    line = Color(0xFF343C42),
    lineStrong = Color(0xFF4D575F),
    accent = Color(0xFF57D4D4),
    accentText = Color(0xFF001517),
    accentSoft = Color(0x2957D4D4),
    wifi = Color(0xFF2AD7D7),
    ir = Color(0xFFFA695C),
    bridge = Color(0xFFF5A84B),
    wifiSoft = Color(0x292AD7D7),
    irSoft = Color(0x29FA695C),
    bridgeSoft = Color(0x29F5A84B),
)

// سمة serene الفاتحة (محوّلة من OKLCH).
val SereneLight = TahakomColors(
    bg = Color(0xFFF3F7FA),
    bg2 = Color(0xFFEDF3F6),
    surface = Color(0xFFFDFFFF),
    surface2 = Color(0xFFF6FBFE),
    text = Color(0xFF1A232B),
    textDim = Color(0xFF525C66),
    textFaint = Color(0xFF808B94),
    line = Color(0xFFDDE2E6),
    lineStrong = Color(0xFFC1C9CE),
    accent = Color(0xFF008B94),
    accentText = Color(0xFFF9FDFD),
    accentSoft = Color(0x1A008B94),
    wifi = Color(0xFF00959C),
    ir = Color(0xFFD33B36),
    bridge = Color(0xFFC67712),
    wifiSoft = Color(0x1A00959C),
    irSoft = Color(0x1AD33B36),
    bridgeSoft = Color(0x1AC67712),
)

val LocalTokens = staticCompositionLocalOf { TahakomTokens(SereneDark, dark = true) }

/** وصول مختصر لرموز التصميم داخل أي Composable. */
val tokens: TahakomTokens
    @Composable @ReadOnlyComposable
    get() = LocalTokens.current
