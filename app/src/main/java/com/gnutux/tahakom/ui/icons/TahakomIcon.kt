package com.gnutux.tahakom.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** يرسم أيقونة خطّية من [TahakomIcons] بلون وحجم محدّدين. */
@Composable
fun TahakomIcon(
    name: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Float = 1.7f,
) {
    val vector = remember(name, tint, strokeWidth) {
        TahakomIcons.vector(name, tint, strokeWidth)
    }
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
