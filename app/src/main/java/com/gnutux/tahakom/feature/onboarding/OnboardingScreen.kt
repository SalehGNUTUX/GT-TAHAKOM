package com.gnutux.tahakom.feature.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gnutux.tahakom.R
import com.gnutux.tahakom.ui.icons.TahakomIcon
import com.gnutux.tahakom.ui.theme.tokens

private data class Slide(val icon: String, val titleRes: Int, val bodyRes: Int)

/**
 * شاشة الترحيب (3 شرائح) — مقتبسة من تصميم GT-TAHAKOM-DESIGN.
 * تُعرض عند أول تشغيل، وتنتهي باستدعاء [onDone].
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val t = tokens; val c = t.colors
    val slides = remember {
        listOf(
            Slide("tv", R.string.ob1_title, R.string.ob1_body),
            Slide("scan", R.string.ob2_title, R.string.ob2_body),
            Slide("link", R.string.ob3_title, R.string.ob3_body),
        )
    }
    var index by remember { mutableIntStateOf(0) }
    val slide = slides[index]
    val last = index == slides.lastIndex

    Column(
        Modifier.fillMaxSize().background(c.bg).systemBarsPadding()
            .padding(horizontal = 26.dp, vertical = 16.dp),
    ) {
        // الترويسة: زر تخطّي
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!last) {
                Text(
                    stringResource(R.string.ob_skip),
                    color = c.textFaint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onDone() }.padding(8.dp),
                )
            }
        }

        // العنصر البصري المتحرك
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            PulsingVisual(slide.icon)
        }

        // النص
        Column(Modifier.height(178.dp)) {
            Text(
                stringResource(slide.titleRes),
                color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(slide.bodyRes),
                color = c.textDim, fontSize = 16.sp, lineHeight = 26.sp,
            )
        }

        // التذييل: نقاط + زر
        Row(
            Modifier.fillMaxWidth().padding(top = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                slides.indices.forEach { i ->
                    Box(
                        Modifier.height(8.dp).size(width = if (i == index) 24.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i == index) c.accent else c.lineStrong),
                    )
                }
            }
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(c.accent)
                    .clickable { if (last) onDone() else index++ }
                    .padding(horizontal = 22.dp, vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (last) R.string.ob_start else R.string.ob_next),
                        color = c.accentText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.size(8.dp))
                    TahakomIcon("caretRight", c.accentText, size = 18.dp, strokeWidth = 2.2f)
                }
            }
        }
    }
}

/** حلقات نابضة حول أيقونة مركزية (مقتبس من om-ring/om-pulse). */
@Composable
private fun PulsingVisual(icon: String) {
    val t = tokens; val c = t.colors
    val transition = rememberInfiniteTransition(label = "ob")
    val scale by transition.animateFloat(
        initialValue = 0.98f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
        label = "ring",
    )
    Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
        listOf(248.dp, 200.dp, 150.dp).forEach { d ->
            Box(
                Modifier.size(d).scale(scale).clip(CircleShape)
                    .border(1.dp, c.line, CircleShape),
            )
        }
        Box(
            Modifier.size(96.dp).clip(RoundedCornerShape(34.dp)).background(c.surface)
                .border(1.dp, c.line, RoundedCornerShape(34.dp)),
            contentAlignment = Alignment.Center,
        ) {
            TahakomIcon(icon, c.accent, size = 42.dp, strokeWidth = 1.6f)
        }
    }
}
