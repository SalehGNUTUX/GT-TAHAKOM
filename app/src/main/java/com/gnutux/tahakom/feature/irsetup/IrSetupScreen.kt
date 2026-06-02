package com.gnutux.tahakom.feature.irsetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.model.Device

/**
 * ضبط جهاز IR مختار: يطبّق منطق "هل الجهاز مشغّل؟" (مطفأ→الطاقة، مشغّل→الصوت/القناة).
 * أزرار الاختبار ظاهرة افتراضياً. عند تأكيد الاستجابة يُفتح ريموت الجهاز.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrSetupScreen(
    irFile: String,
    onBack: () -> Unit,
    onDeviceReady: (Device) -> Unit,
    viewModel: IrSetupViewModel = hiltViewModel(),
) {
    LaunchedEffect(irFile) { viewModel.load(irFile) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.brand ?: stringResource(R.string.ir_setup_title)) },
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
            if (!state.irAvailable) {
                Text(
                    stringResource(R.string.ir_no_emitter),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }

            Text(
                stringResource(R.string.ir_power_question),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            // إشارات الاختبار — مع أيقونات تعبيرية تسهّل الفهم.
            Button(
                onClick = { viewModel.sendTest(PowerState.OFF) },
                enabled = state.irAvailable,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("⏻  " + stringResource(R.string.ir_test_power)) }

            OutlinedButton(
                onClick = { viewModel.sendTest(PowerState.ON) },
                enabled = state.irAvailable,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("🔊  " + stringResource(R.string.ir_test_volume)) }

            state.testButtonLabel?.let {
                Text(
                    stringResource(R.string.ir_sent_signal, it),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                stringResource(R.string.ir_responded_question),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            // زرّان تعبيريان: ✓ نعم اعتمد هذا الجهاز · ✗ اختر علامة أخرى.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExpressiveChoice(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Check,
                    label = stringResource(R.string.ir_yes_responded),
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                    onClick = { viewModel.confirmDevice()?.let(onDeviceReady) },
                )
                ExpressiveChoice(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Close,
                    label = stringResource(R.string.ir_pick_other),
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurface,
                    onClick = onBack,
                )
            }
        }
    }
}

/** زر اختيار تعبيري: أيقونة كبيرة + نص، يؤدّي دور الزر النصّي. */
@Composable
private fun ExpressiveChoice(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = container),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(34.dp))
            Text(label, color = content, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
        }
    }
}
