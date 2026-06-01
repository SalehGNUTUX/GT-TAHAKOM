package com.gnutux.tahakom.feature.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.ui.icons.TahakomIcon
import com.gnutux.tahakom.ui.theme.tokens
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.hypot

/**
 * شاشة الريموت بالتصميم الجديد (GT-TAHAKOM-DESIGN): أزرار دائرية بأيقونات حقيقية،
 * شريط الوسيلة النشطة، لوحة لمس بإيماءات، روكرات الصوت/القناة، أزرار الوسائط.
 * تُظهر الأزرار المدعومة فقط. تنتظر جاهزية أكواد IR قبل الإرسال (يحلّ مشكلة النقر مرتين).
 */
@Composable
fun RemoteScreen(
    device: Device,
    onBack: () -> Unit,
    viewModel: RemoteViewModel = hiltViewModel(),
) {
    val t = tokens
    val c = t.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(device.id) { viewModel.bind(device) }

    fun key(button: ButtonId, label: String) = viewModel.send(device, button, label)
    val has = { b: ButtonId -> b in state.supported }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(
        Modifier.fillMaxSize().background(c.bg).padding(horizontal = 16.dp),
    ) {
        // الترويسة
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhostIconBtn(if (rtl) "forwardNav" else "back", c.text) { onBack() }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(device.name, color = c.text, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
                device.metadata["brand"]?.let {
                    Text(it, color = c.textFaint, fontSize = 12.5.sp)
                }
            }
            Spacer(Modifier.size(40.dp))
        }

        // شريط الوسيلة النشطة
        TransportPill(device)

        Spacer(Modifier.height(12.dp))

        // مجموعة الأزرار العلوية
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (has(ButtonId.POWER)) RoundIconBtn("power", accent = true) { key(ButtonId.POWER, "Power") }
            if (has(ButtonId.HOME)) RoundIconBtn("homeBtn") { key(ButtonId.HOME, "Home") }
            if (has(ButtonId.MENU)) RoundIconBtn("menu") { key(ButtonId.MENU, "Menu") }
            if (has(ButtonId.BACK)) RoundIconBtn(if (rtl) "forwardNav" else "back") { key(ButtonId.BACK, "Back") }
            if (has(ButtonId.SOURCE)) RoundIconBtn("source") { key(ButtonId.SOURCE, "Source") }
        }

        // لوحة اللمس (إن دُعم أي اتجاه)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (listOf(ButtonId.NAV_UP, ButtonId.NAV_DOWN, ButtonId.NAV_LEFT, ButtonId.NAV_RIGHT, ButtonId.NAV_OK).any(has)) {
                TouchPad(
                    onUp = { key(ButtonId.NAV_UP, "Up") },
                    onDown = { key(ButtonId.NAV_DOWN, "Down") },
                    onLeft = { key(ButtonId.NAV_LEFT, "Left") },
                    onRight = { key(ButtonId.NAV_RIGHT, "Right") },
                    onOk = { key(ButtonId.NAV_OK, "OK") },
                )
            }
        }

        // الروكرات: الصوت + القناة
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (has(ButtonId.VOL_UP) || has(ButtonId.VOL_DOWN)) {
                Rocker(
                    Modifier.weight(1f),
                    topIcon = "volUp", bottomIcon = "volDown",
                    midIcon = if (has(ButtonId.MUTE)) "mute" else null,
                    label = stringResource(R.string.rm_volume),
                    onUp = { key(ButtonId.VOL_UP, "Vol+") },
                    onDown = { key(ButtonId.VOL_DOWN, "Vol−") },
                    onMid = { key(ButtonId.MUTE, "Mute") },
                )
            }
            if (has(ButtonId.CH_UP) || has(ButtonId.CH_DOWN)) {
                Rocker(
                    Modifier.weight(1f),
                    topIcon = "caretUp", bottomIcon = "caretDown",
                    midIcon = null,
                    label = stringResource(R.string.rm_channel),
                    onUp = { key(ButtonId.CH_UP, "Ch+") },
                    onDown = { key(ButtonId.CH_DOWN, "Ch−") },
                    onMid = {},
                )
            }
        }

        // الوسائط
        if (listOf(ButtonId.RWD, ButtonId.PLAY, ButtonId.FFWD).any(has)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                if (has(ButtonId.RWD)) RoundIconBtn(if (rtl) "forward" else "rewind") { key(ButtonId.RWD, "Rwd") }
                if (has(ButtonId.PLAY)) RoundIconBtn("play", surface2 = true) { key(ButtonId.PLAY, "Play") }
                if (has(ButtonId.FFWD)) RoundIconBtn(if (rtl) "rewind" else "forward") { key(ButtonId.FFWD, "Ffwd") }
            }
        }

        // تغذية راجعة
        state.lastError?.let {
            Text(
                "⚠ $it", color = c.ir, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun TransportPill(device: Device) {
    val t = tokens; val c = t.colors
    val (tcol, soft, icon) = when (device.transport.name) {
        "IR" -> Triple(c.ir, c.irSoft, "ir")
        "BROADLINK" -> Triple(c.bridge, c.bridgeSoft, "bridge")
        else -> Triple(c.wifi, c.wifiSoft, "wifi")
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(t.shape.md)).background(soft)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            TahakomIcon(icon, tcol, size = 22.dp, strokeWidth = 2f)
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(stringResource(R.string.rm_active_path), color = tcol, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(transportLabel(device), color = tcol, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun transportLabel(device: Device): String = when (device.transport.name) {
    "IR" -> stringResource(R.string.tr_ir)
    "BROADLINK" -> stringResource(R.string.tr_bridge)
    else -> stringResource(R.string.tr_wifi)
}

@Composable
private fun RoundIconBtn(
    icon: String,
    accent: Boolean = false,
    surface2: Boolean = false,
    onClick: () -> Unit,
) {
    val t = tokens; val c = t.colors
    val bg = when { accent -> c.accent; surface2 -> c.surface2; else -> c.surface }
    val fg = if (accent) c.accentText else c.text
    Box(
        Modifier.size(54.dp).clip(CircleShape).background(bg)
            .border(if (accent) 0.dp else 1.dp, if (accent) Color.Transparent else c.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TahakomIcon(icon, fg, size = 23.dp, strokeWidth = 1.8f)
    }
}

@Composable
private fun GhostIconBtn(icon: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TahakomIcon(icon, tint, size = 22.dp)
    }
}

@Composable
private fun Rocker(
    modifier: Modifier = Modifier,
    topIcon: String,
    bottomIcon: String,
    midIcon: String?,
    label: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onMid: () -> Unit,
) {
    val t = tokens; val c = t.colors
    Row(
        modifier.clip(RoundedCornerShape(t.shape.lg)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(t.shape.lg)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).clickable(onClick = onUp).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            TahakomIcon(topIcon, c.text, size = 22.dp, strokeWidth = 1.9f)
        }
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (midIcon != null) {
                Box(Modifier.clickable(onClick = onMid).padding(8.dp), contentAlignment = Alignment.Center) {
                    TahakomIcon(midIcon, c.textDim, size = 20.dp)
                }
            } else {
                Text(label, color = c.textFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.weight(1f).clickable(onClick = onDown).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            TahakomIcon(bottomIcon, c.text, size = 22.dp, strokeWidth = 1.9f)
        }
    }
}

/** لوحة لمس دائرية بإيماءات: نقر=OK، سحب=اتجاه. */
@Composable
private fun TouchPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
) {
    val t = tokens; val c = t.colors
    Box(
        Modifier.size(280.dp).clip(CircleShape).background(c.surface2)
            .border(1.dp, c.line, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onOk() })
            }
            .pointerInput(Unit) {
                var acc = Offset.Zero
                detectDragGestures(
                    onDragStart = { acc = Offset.Zero },
                    onDrag = { _, d -> acc += d },
                    onDragEnd = {
                        if (hypot(acc.x, acc.y) > 40f) {
                            if (abs(acc.x) > abs(acc.y)) {
                                if (acc.x > 0) onRight() else onLeft()
                            } else {
                                if (acc.y > 0) onDown() else onUp()
                            }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // أيقونات الحواف
        TahakomIcon("caretUp", c.textFaint, Modifier.align(Alignment.TopCenter).padding(top = 14.dp), size = 22.dp, strokeWidth = 2f)
        TahakomIcon("caretDown", c.textFaint, Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), size = 22.dp, strokeWidth = 2f)
        TahakomIcon("caretLeft", c.textFaint, Modifier.align(Alignment.CenterStart).padding(start = 14.dp), size = 22.dp, strokeWidth = 2f)
        TahakomIcon("caretRight", c.textFaint, Modifier.align(Alignment.CenterEnd).padding(end = 14.dp), size = 22.dp, strokeWidth = 2f)
        // زر OK المركزي
        Box(
            Modifier.size(92.dp).clip(CircleShape).background(c.bg)
                .border(1.5.dp, c.line, CircleShape)
                .clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onOk),
            contentAlignment = Alignment.Center,
        ) {
            Text("OK", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}
