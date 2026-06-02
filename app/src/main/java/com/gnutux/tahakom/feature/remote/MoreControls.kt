package com.gnutux.tahakom.feature.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.ui.icons.TahakomIcon
import com.gnutux.tahakom.ui.theme.tokens

/**
 * محتوى قائمة "المزيد" (تُفتح بزر الثلاث نقاط) — مقتبسة من MoreControls بالتصميم.
 * **عامة لكل جهاز**: تعرض فقط ما يدعمه الجهاز من أرقام/وظائف/أزرار ملوّنة.
 */
@Composable
fun MoreControls(
    supported: Set<ButtonId>,
    onSend: (ButtonId, String) -> Unit,
) {
    val c = tokens.colors
    val has = { b: ButtonId -> b in supported }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            stringResource(R.string.more_title),
            color = c.text, fontSize = 19.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 14.dp),
        )

        // لوحة الأرقام (إن دُعم أي رقم)
        val digits = listOf(
            ButtonId.DIGIT_1, ButtonId.DIGIT_2, ButtonId.DIGIT_3,
            ButtonId.DIGIT_4, ButtonId.DIGIT_5, ButtonId.DIGIT_6,
            ButtonId.DIGIT_7, ButtonId.DIGIT_8, ButtonId.DIGIT_9,
        )
        if (digits.any(has) || has(ButtonId.DIGIT_0)) {
            SectionLabel(stringResource(R.string.more_numbers))
            // صفوف 3×3 ثم صف 0
            digits.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d ->
                        DigitKey(Modifier.weight(1f), d.name.removePrefix("DIGIT_"), enabled = has(d)) {
                            onSend(d, d.name.removePrefix("DIGIT_"))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.weight(1f))
                DigitKey(Modifier.weight(1f), "0", enabled = has(ButtonId.DIGIT_0)) { onSend(ButtonId.DIGIT_0, "0") }
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
        }

        // الوظائف (تعرض المدعوم فقط)
        val funcs = listOf(
            Triple(ButtonId.GUIDE, "guide", R.string.fn_guide),
            Triple(ButtonId.LIST, "menu", R.string.fn_list),
            Triple(ButtonId.INFO, "info", R.string.fn_info),
            Triple(ButtonId.CC, "subtitles", R.string.fn_subs),
            Triple(ButtonId.SMART, "tv", R.string.fn_smart),
            Triple(ButtonId.APPS, "tv", R.string.fn_smart),
            Triple(ButtonId.SEARCH, "search", R.string.fn_search),
            Triple(ButtonId.TOOLS, "gear", R.string.fn_tools),
            Triple(ButtonId.SETTINGS, "gear", R.string.fn_settings),
            Triple(ButtonId.SOURCE, "source", R.string.fn_source),
            Triple(ButtonId.EXIT, "close", R.string.fn_exit),
            Triple(ButtonId.TEXT, "subtitles", R.string.fn_text),
        ).filter { has(it.first) }.distinctBy { it.third } // تجنّب تكرار "ذكية" مرتين
        if (funcs.isNotEmpty()) {
            SectionLabel(stringResource(R.string.more_functions))
            funcs.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (id, icon, labelRes) ->
                        FuncKey(Modifier.weight(1f), icon, stringResource(labelRes)) { onSend(id, id.name) }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // التحكّم بالوسائط (تشغيل/إيقاف/توقف/تسجيل) — يعرض المدعوم فقط
        val media = listOf(
            Triple(ButtonId.RWD, "rewind", R.string.md_rwd),
            Triple(ButtonId.PLAY, "play", R.string.md_play),
            Triple(ButtonId.PAUSE, "pause", R.string.md_pause),
            Triple(ButtonId.STOP, "close", R.string.md_stop),
            Triple(ButtonId.FFWD, "forward", R.string.md_ffwd),
            Triple(ButtonId.RECORD, "info", R.string.md_rec),
        ).filter { has(it.first) }
        if (media.isNotEmpty()) {
            SectionLabel(stringResource(R.string.more_media))
            media.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (id, icon, labelRes) ->
                        FuncKey(Modifier.weight(1f), icon, stringResource(labelRes)) { onSend(id, id.name) }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // الأزرار الملوّنة
        val colors = listOf(
            ButtonId.RED to Color(0xFFE5484D),
            ButtonId.GREEN to Color(0xFF30A46C),
            ButtonId.YELLOW to Color(0xFFFFC53D),
            ButtonId.BLUE to Color(0xFF3E63DD),
        ).filter { has(it.first) }
        if (colors.isNotEmpty()) {
            SectionLabel(stringResource(R.string.more_colors))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colors.forEach { (id, col) ->
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(col).clickable { onSend(id, id.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = tokens.colors.textFaint, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun DigitKey(modifier: Modifier, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = tokens.colors
    Box(
        modifier.aspectRatio(1.6f).clip(RoundedCornerShape(tokens.shape.md))
            .background(if (enabled) c.surface else c.bg2)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.md))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) c.text else c.textFaint, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FuncKey(modifier: Modifier, icon: String, label: String, onClick: () -> Unit) {
    val c = tokens.colors
    Column(
        modifier.clip(RoundedCornerShape(tokens.shape.md)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.md))
            .clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        TahakomIcon(icon, c.text, size = 22.dp)
        Text(label, color = c.textDim, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
