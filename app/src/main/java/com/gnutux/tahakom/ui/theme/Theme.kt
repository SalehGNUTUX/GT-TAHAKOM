package com.gnutux.tahakom.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * سمة GT-TAHAKOM — تعتمد نظام رموز التصميم serene (مقتبس من GT-TAHAKOM-DESIGN).
 * توفّر [LocalTokens] للوصول للألوان الموسّعة، وتُسقطها على Material 3 ColorScheme
 * للمكوّنات القياسية. اتجاه RTL يتولّاه Compose تلقائياً حسب اللغة.
 */
@Composable
fun TahakomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (darkTheme) SereneDark else SereneLight
    val tokens = TahakomTokens(colors = c, dark = darkTheme)

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.accent,
            onPrimary = c.accentText,
            primaryContainer = c.accentSoft,
            onPrimaryContainer = c.accent,
            secondary = c.bridge,
            tertiary = c.wifi,
            background = c.bg,
            onBackground = c.text,
            surface = c.surface,
            onSurface = c.text,
            surfaceVariant = c.surface2,
            onSurfaceVariant = c.textDim,
            outline = c.line,
            outlineVariant = c.lineStrong,
            error = c.ir,
        )
    } else {
        lightColorScheme(
            primary = c.accent,
            onPrimary = c.accentText,
            primaryContainer = c.accentSoft,
            onPrimaryContainer = c.accent,
            secondary = c.bridge,
            tertiary = c.wifi,
            background = c.bg,
            onBackground = c.text,
            surface = c.surface,
            onSurface = c.text,
            surfaceVariant = c.surface2,
            onSurfaceVariant = c.textDim,
            outline = c.line,
            outlineVariant = c.lineStrong,
            error = c.ir,
        )
    }

    CompositionLocalProvider(LocalTokens provides tokens) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            content = content,
        )
    }
}
