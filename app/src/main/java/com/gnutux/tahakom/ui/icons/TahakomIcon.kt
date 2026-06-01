package com.gnutux.tahakom.ui.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** يرسم أيقونة من [TahakomIcons] (Material Outlined) بلون وحجم محدّدين. */
@Composable
fun TahakomIcon(
    name: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Float = 1.7f, // مُتجاهَل (للتوافق مع نداءات سابقة)
) {
    Icon(
        imageVector = TahakomIcons.vector(name),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}
