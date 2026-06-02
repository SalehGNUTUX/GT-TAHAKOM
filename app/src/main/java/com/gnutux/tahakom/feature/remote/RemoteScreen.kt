package com.gnutux.tahakom.feature.remote

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.ui.icons.TahakomIcon
import com.gnutux.tahakom.ui.theme.tokens

/**
 * شاشة الريموت — **عامة ومدفوعة بالبيانات**: تعمل مع أي جهاز وتعرض تلقائياً
 * الأزرار التي يدعمها فقط. الأزرار الأساسية في الشاشة، والبقية في قائمة "المزيد".
 * أزرار الاتجاهات قابلة للنقر فعلياً (إضافةً للسحب).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    device: Device,
    onBack: () -> Unit,
    viewModel: RemoteViewModel = hiltViewModel(),
) {
    val t = tokens; val c = t.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(device.id) { viewModel.bind(device) }

    fun key(button: ButtonId, label: String) = viewModel.send(device, button, label)
    val has = { b: ButtonId -> b in state.supported }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var showMore by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(c.bg).systemBarsPadding().padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // الترويسة: رجوع · الاسم · مصدر · المزيد
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhostIconBtn(if (rtl) "forwardNav" else "back", c.text) { onBack() }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(device.name, color = c.text, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
                device.metadata["brand"]?.let { Text(it, color = c.textFaint, fontSize = 12.5.sp) }
            }
            if (has(ButtonId.SOURCE)) GhostIconBtn("source", c.text) { key(ButtonId.SOURCE, "Source") }
            // زر "المزيد" (ثلاث نقاط) — يفتح قائمة الأزرار الإضافية
            GhostIconBtn("more", c.text) { showMore = true }
        }

        TransportPill(device)
        Spacer(Modifier.height(12.dp))

        // مجموعة علوية: طاقة · رئيسية · قائمة · رجوع
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (has(ButtonId.POWER)) RoundIconBtn("power", accent = true) { key(ButtonId.POWER, "Power") }
            if (has(ButtonId.HOME)) RoundIconBtn("homeBtn") { key(ButtonId.HOME, "Home") }
            if (has(ButtonId.MENU)) RoundIconBtn("menu") { key(ButtonId.MENU, "Menu") }
            if (has(ButtonId.BACK)) RoundIconBtn(if (rtl) "forwardNav" else "back") { key(ButtonId.BACK, "Back") }
        }

        // لوحة الاتجاهات — أزرار قابلة للنقر (إضافةً للإيماءات)
        if (listOf(ButtonId.NAV_UP, ButtonId.NAV_DOWN, ButtonId.NAV_LEFT, ButtonId.NAV_RIGHT, ButtonId.NAV_OK).any(has)) {
            Spacer(Modifier.height(16.dp))
            DPad(
                has = has,
                onUp = { key(ButtonId.NAV_UP, "Up") },
                onDown = { key(ButtonId.NAV_DOWN, "Down") },
                onLeft = { key(ButtonId.NAV_LEFT, "Left") },
                onRight = { key(ButtonId.NAV_RIGHT, "Right") },
                onOk = { key(ButtonId.NAV_OK, "OK") },
            )
            Spacer(Modifier.height(16.dp))
        }

        // صف سياقي: القائمة الذكية/التطبيقات + قائمة القنوات + بحث (يظهر المدعوم فقط)
        if (listOf(ButtonId.APPS, ButtonId.SMART, ButtonId.LIST, ButtonId.GUIDE, ButtonId.SEARCH).any(has)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                if (has(ButtonId.APPS) || has(ButtonId.SMART)) {
                    PillBtn("tv", stringResource(R.string.fn_smart)) {
                        key(if (has(ButtonId.APPS)) ButtonId.APPS else ButtonId.SMART, "Apps")
                    }
                }
                if (has(ButtonId.LIST)) PillBtn("menu", stringResource(R.string.fn_list)) { key(ButtonId.LIST, "List") }
                if (has(ButtonId.SEARCH)) PillBtn("search", stringResource(R.string.fn_search)) { key(ButtonId.SEARCH, "Search") }
            }
        }

        // الروكرات: الصوت + القناة
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (has(ButtonId.VOL_UP) || has(ButtonId.VOL_DOWN)) {
                Rocker(
                    Modifier.weight(1f), "volUp", "volDown",
                    if (has(ButtonId.MUTE)) "mute" else null,
                    stringResource(R.string.rm_volume),
                    { key(ButtonId.VOL_UP, "Vol+") }, { key(ButtonId.VOL_DOWN, "Vol−") }, { key(ButtonId.MUTE, "Mute") },
                )
            }
            if (has(ButtonId.CH_UP) || has(ButtonId.CH_DOWN)) {
                Rocker(
                    Modifier.weight(1f), "caretUp", "caretDown", null,
                    stringResource(R.string.rm_channel),
                    { key(ButtonId.CH_UP, "Ch+") }, { key(ButtonId.CH_DOWN, "Ch−") }, {},
                )
            }
        }

        // الوسائط
        if (listOf(ButtonId.RWD, ButtonId.PLAY, ButtonId.PAUSE, ButtonId.FFWD).any(has)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                if (has(ButtonId.RWD)) RoundIconBtn(if (rtl) "forward" else "rewind") { key(ButtonId.RWD, "Rwd") }
                if (has(ButtonId.PLAY)) RoundIconBtn("play", surface2 = true) { key(ButtonId.PLAY, "Play") }
                if (has(ButtonId.PAUSE)) RoundIconBtn("pause", surface2 = true) { key(ButtonId.PAUSE, "Pause") }
                if (has(ButtonId.FFWD)) RoundIconBtn(if (rtl) "rewind" else "forward") { key(ButtonId.FFWD, "Ffwd") }
            }
        }

        // زر "أزرار إضافية" واضح (إضافةً لزر الثلاث نقاط في الأعلى) — يفتح نفس القائمة.
        Box(
            Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(50))
                .background(c.surface).border(1.dp, c.line, RoundedCornerShape(50))
                .clickable { showMore = true }.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TahakomIcon("more", c.text, size = 20.dp)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.more_open), color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        state.lastError?.let {
            Text("⚠ $it", color = c.ir, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(8.dp))
    }

    // قائمة "المزيد" (ثلاث نقاط): أرقام + وظائف + ألوان — تعرض المدعوم فقط
    if (showMore) {
        ModalBottomSheet(
            onDismissRequest = { showMore = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.bg,
        ) {
            MoreControls(state.supported, onSend = { b, l -> key(b, l) })
            Spacer(Modifier.height(24.dp))
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
            TahakomIcon(icon, tcol, size = 22.dp)
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
private fun RoundIconBtn(icon: String, accent: Boolean = false, surface2: Boolean = false, onClick: () -> Unit) {
    val c = tokens.colors
    val bg = when { accent -> c.accent; surface2 -> c.surface2; else -> c.surface }
    val fg = if (accent) c.accentText else c.text
    Box(
        Modifier.size(54.dp).clip(CircleShape).background(bg)
            .border(if (accent) 0.dp else 1.dp, if (accent) Color.Transparent else c.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { TahakomIcon(icon, fg, size = 24.dp) }
}

@Composable
private fun PillBtn(icon: String, label: String, onClick: () -> Unit) {
    val c = tokens.colors
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TahakomIcon(icon, c.text, size = 18.dp)
        Spacer(Modifier.size(6.dp))
        Text(label, color = c.text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GhostIconBtn(icon: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { TahakomIcon(icon, tint, size = 22.dp) }
}

/** لوحة اتجاهات: 5 أزرار دائرية قابلة للنقر (فوق/تحت/يسار/يمين/OK). مواضع ثابتة لا تنعكس. */
@Composable
private fun DPad(
    has: (ButtonId) -> Boolean,
    onUp: () -> Unit, onDown: () -> Unit, onLeft: () -> Unit, onRight: () -> Unit, onOk: () -> Unit,
) {
    // الاتجاهات فيزيائية لا لغوية: نُجبر LTR كي لا تنعكس يمين/يسار في العربية.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (has(ButtonId.NAV_UP)) NavKey("caretUp", onUp) else Spacer(Modifier.size(60.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (has(ButtonId.NAV_LEFT)) NavKey("caretLeft", onLeft) else Spacer(Modifier.size(60.dp))
                if (has(ButtonId.NAV_OK)) OkKey(onOk) else Spacer(Modifier.size(76.dp))
                if (has(ButtonId.NAV_RIGHT)) NavKey("caretRight", onRight) else Spacer(Modifier.size(60.dp))
            }
            if (has(ButtonId.NAV_DOWN)) NavKey("caretDown", onDown) else Spacer(Modifier.size(60.dp))
        }
    }
}

@Composable
private fun NavKey(icon: String, onClick: () -> Unit) {
    val c = tokens.colors
    Box(
        Modifier.size(60.dp).clip(CircleShape).background(c.surface)
            .border(1.dp, c.line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { TahakomIcon(icon, c.text, size = 28.dp) }
}

@Composable
private fun OkKey(onClick: () -> Unit) {
    val c = tokens.colors
    Box(
        Modifier.size(76.dp).clip(CircleShape).background(c.accent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("OK", color = c.accentText, fontSize = 18.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun Rocker(
    modifier: Modifier = Modifier,
    topIcon: String, bottomIcon: String, midIcon: String?, label: String,
    onUp: () -> Unit, onDown: () -> Unit, onMid: () -> Unit,
) {
    val t = tokens; val c = t.colors
    Row(
        modifier.clip(RoundedCornerShape(t.shape.lg)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(t.shape.lg)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).clickable(onClick = onUp).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            TahakomIcon(topIcon, c.text, size = 22.dp)
        }
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (midIcon != null) {
                Box(Modifier.clip(CircleShape).clickable(onClick = onMid).padding(8.dp), contentAlignment = Alignment.Center) {
                    TahakomIcon(midIcon, c.textDim, size = 20.dp)
                }
            } else Text(label, color = c.textFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.weight(1f).clickable(onClick = onDown).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            TahakomIcon(bottomIcon, c.text, size = 22.dp)
        }
    }
}
