package com.gnutux.tahakom.feature.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Device

/**
 * شاشة الريموت — أزرار تعمل فعلاً. تُظهر فقط الأزرار التي يدعمها الجهاز
 * (مهم لأجهزة IR: لا كل جهاز يملك كل الأزرار). تستدعي [RemoteViewModel.bind]
 * لتحميل أكواد IR وترجمة الأزرار الدلالية إلى Pronto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    device: Device,
    onBack: () -> Unit,
    viewModel: RemoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(device.id) { viewModel.bind(device) }

    fun key(button: ButtonId, label: String) = viewModel.send(device, button, label)
    val has = { b: ButtonId -> b in state.supported }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // صف الطاقة + الرئيسية + الرجوع (أظهر المتاح فقط)
            RowOf {
                if (has(ButtonId.POWER)) RoundKey("⏻", accent = true) { key(ButtonId.POWER, "Power") }
                if (has(ButtonId.HOME)) RoundKey("⌂") { key(ButtonId.HOME, "Home") }
                if (has(ButtonId.BACK)) RoundKey("↩") { key(ButtonId.BACK, "Back") }
            }

            // لوحة الاتجاهات (تظهر إن دُعم أي اتجاه)
            if (listOf(ButtonId.NAV_UP, ButtonId.NAV_DOWN, ButtonId.NAV_LEFT, ButtonId.NAV_RIGHT, ButtonId.NAV_OK).any(has)) {
                DPad(
                    has = has,
                    onUp = { key(ButtonId.NAV_UP, "Up") },
                    onDown = { key(ButtonId.NAV_DOWN, "Down") },
                    onLeft = { key(ButtonId.NAV_LEFT, "Left") },
                    onRight = { key(ButtonId.NAV_RIGHT, "Right") },
                    onOk = { key(ButtonId.NAV_OK, "OK") },
                )
            }

            // الصوت + الكتم + القنوات
            RowOf {
                if (has(ButtonId.VOL_DOWN) || has(ButtonId.VOL_UP)) {
                    LabeledPair(
                        "VOL",
                        left = if (has(ButtonId.VOL_DOWN)) ("−" to { key(ButtonId.VOL_DOWN, "Vol−") }) else null,
                        right = if (has(ButtonId.VOL_UP)) ("+" to { key(ButtonId.VOL_UP, "Vol+") }) else null,
                    )
                }
                if (has(ButtonId.MUTE)) RoundKey("🔇") { key(ButtonId.MUTE, "Mute") }
                if (has(ButtonId.CH_DOWN) || has(ButtonId.CH_UP)) {
                    LabeledPair(
                        "CH",
                        left = if (has(ButtonId.CH_DOWN)) ("−" to { key(ButtonId.CH_DOWN, "Ch−") }) else null,
                        right = if (has(ButtonId.CH_UP)) ("+" to { key(ButtonId.CH_UP, "Ch+") }) else null,
                    )
                }
            }

            // الوسائط
            if (listOf(ButtonId.RWD, ButtonId.PLAY, ButtonId.PAUSE, ButtonId.FFWD).any(has)) {
                RowOf {
                    if (has(ButtonId.RWD)) RoundKey("⏪") { key(ButtonId.RWD, "Rwd") }
                    if (has(ButtonId.PLAY)) RoundKey("⏯") { key(ButtonId.PLAY, "Play") }
                    if (has(ButtonId.FFWD)) RoundKey("⏩") { key(ButtonId.FFWD, "Ffwd") }
                }
            }

            // أزرار إضافية
            RowOf {
                if (has(ButtonId.INFO)) FilledTonalButton(onClick = { key(ButtonId.INFO, "Info") }) { Text("ℹ") }
                if (has(ButtonId.MENU)) FilledTonalButton(onClick = { key(ButtonId.MENU, "Menu") }) { Text("☰") }
                if (has(ButtonId.SOURCE)) FilledTonalButton(onClick = { key(ButtonId.SOURCE, "Source") }) { Text("⮂") }
            }

            Spacer(Modifier.height(8.dp))
            when {
                state.lastError != null -> Text(
                    "⚠ ${state.lastError}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                state.lastSentLabel != null -> Text(
                    "✓ ${state.lastSentLabel}",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun RowOf(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        content()
    }
}

@Composable
private fun LabeledPair(
    label: String,
    left: Pair<String, () -> Unit>?,
    right: Pair<String, () -> Unit>?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            left?.let { RoundKey(it.first, onClick = it.second) }
            right?.let { RoundKey(it.first, onClick = it.second) }
        }
    }
}

@Composable
private fun RoundKey(label: String, accent: Boolean = false, onClick: () -> Unit) {
    Button(onClick = onClick, shape = CircleShape, modifier = Modifier.size(64.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DPad(
    has: (ButtonId) -> Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (has(ButtonId.NAV_UP)) RoundKey("▲", onClick = onUp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (has(ButtonId.NAV_LEFT)) RoundKey("◀", onClick = onLeft)
            if (has(ButtonId.NAV_OK)) RoundKey("OK", onClick = onOk)
            if (has(ButtonId.NAV_RIGHT)) RoundKey("▶", onClick = onRight)
        }
        if (has(ButtonId.NAV_DOWN)) RoundKey("▼", onClick = onDown)
    }
}
