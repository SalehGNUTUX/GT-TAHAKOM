package com.gnutux.tahakom.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = BrandCyanContainer,
    secondary = BrandOrange,
    tertiary = BrandOrange,
)

private val DarkColors = darkColorScheme(
    primary = BrandCyan,
    primaryContainer = BrandCyanContainer,
    secondary = BrandOrange,
    tertiary = BrandOrange,
    background = BrandDark,
    surface = BrandSurfaceDark,
    onBackground = BrandOnDark,
    onSurface = BrandOnDark,
)

/**
 * سمة GT-TAHAKOM. تعتمد الألوان الديناميكية (Material You) على أندرويد 12+،
 * وإلا تعود لألوان النيلي الاحتياطية. اتجاه RTL يتولّاه Compose تلقائياً حسب اللغة.
 */
@Composable
fun TahakomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
