package com.gnutux.tahakom.feature.irsetup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
 * شاشة ضبط جهاز IR شبه الآلي. تطبّق منطق "هل الجهاز مشغّل؟":
 * مطفأ → زر الطاقة، مشغّل → زر الصوت/القناة. ينتقل المستخدم بين العلامات
 * حتى يستجيب جهازه ثم يؤكّد.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrSetupScreen(
    onBack: () -> Unit,
    onDeviceReady: (Device) -> Unit,
    viewModel: IrSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ir_setup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (!state.irAvailable) {
                Text(
                    stringResource(R.string.ir_no_emitter),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            when {
                state.category == null -> CategoryPicker(state.categories, viewModel::selectCategory)
                else -> BrandTester(
                    state = state,
                    onTest = viewModel::sendTest,
                    onNext = viewModel::nextBrand,
                    onPrev = viewModel::previousBrand,
                    onConfirm = { viewModel.confirmDevice()?.let(onDeviceReady) },
                )
            }
        }
    }
}

@Composable
private fun CategoryPicker(categories: List<String>, onSelect: (String) -> Unit) {
    Column {
        Text(
            stringResource(R.string.ir_pick_category),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn {
            items(categories, key = { it }) { cat ->
                ElevatedCard(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(cat) },
                ) {
                    Text(cat, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun BrandTester(
    state: IrSetupUiState,
    onTest: (PowerState) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.ir_brand_progress, state.testIndex + 1, state.devices.size),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            state.testingBrand ?: "",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            stringResource(R.string.ir_power_question),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        // مطفأ → اختبار بزر الطاقة
        Button(onClick = { onTest(PowerState.OFF) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ir_test_power))
        }
        // مشغّل → اختبار بزر الصوت/القناة
        OutlinedButton(onClick = { onTest(PowerState.ON) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ir_test_volume))
        }

        state.testButtonLabel?.let {
            Text(
                stringResource(R.string.ir_sent_signal, it),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text(
            stringResource(R.string.ir_responded_question),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ir_yes_responded))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.ir_prev_brand))
            }
            OutlinedButton(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.ir_next_brand))
            }
        }
    }
}
