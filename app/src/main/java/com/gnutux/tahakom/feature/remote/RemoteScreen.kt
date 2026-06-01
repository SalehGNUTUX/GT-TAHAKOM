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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Command
import com.gnutux.tahakom.core.model.Device

/**
 * شاشة الريموت — أزرار تعمل فعلاً وترسل أوامر للجهاز عبر [RemoteViewModel].
 * تخطيط تلفاز قياسي (طاقة، تنقّل، صوت، قنوات، وسائط) على غرار IRRemote.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    device: Device,
    onBack: () -> Unit,
    viewModel: RemoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    fun key(button: ButtonId, label: String) =
        viewModel.send(device, Command.Key(button), label)

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
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // صف الطاقة + الرئيسية + الرجوع
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RoundKey("⏻") { key(ButtonId.POWER, "Power") }
                RoundKey("⌂") { key(ButtonId.HOME, "Home") }
                RoundKey("↩") { key(ButtonId.BACK, "Back") }
            }

            // لوحة الاتجاهات
            DPad(
                onUp = { key(ButtonId.NAV_UP, "Up") },
                onDown = { key(ButtonId.NAV_DOWN, "Down") },
                onLeft = { key(ButtonId.NAV_LEFT, "Left") },
                onRight = { key(ButtonId.NAV_RIGHT, "Right") },
                onOk = { key(ButtonId.NAV_OK, "OK") },
            )

            // الصوت + القنوات
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VOL", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoundKey("−") { key(ButtonId.VOL_DOWN, "Vol−") }
                        RoundKey("+") { key(ButtonId.VOL_UP, "Vol+") }
                    }
                }
                RoundKey("🔇") { key(ButtonId.MUTE, "Mute") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CH", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoundKey("−") { key(ButtonId.CH_DOWN, "Ch−") }
                        RoundKey("+") { key(ButtonId.CH_UP, "Ch+") }
                    }
                }
            }

            // الوسائط
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RoundKey("⏪") { key(ButtonId.RWD, "Rwd") }
                RoundKey("⏯") { key(ButtonId.PLAY, "Play") }
                RoundKey("⏩") { key(ButtonId.FFWD, "Ffwd") }
            }

            // أزرار إضافية
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilledTonalButton(onClick = { key(ButtonId.INFO, "Info") }) { Text("ℹ") }
                FilledTonalButton(onClick = { key(ButtonId.MENU, "Menu") }) { Text("☰") }
                FilledTonalButton(onClick = { key(ButtonId.SOURCE, "Source") }) { Text("⮂") }
            }

            Spacer(Modifier.height(8.dp))
            // تغذية راجعة
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
private fun RoundKey(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, shape = CircleShape, modifier = Modifier.size(64.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RoundKey("▲", onUp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundKey("◀", onLeft)
            RoundKey("OK", onOk)
            RoundKey("▶", onRight)
        }
        RoundKey("▼", onDown)
    }
}
