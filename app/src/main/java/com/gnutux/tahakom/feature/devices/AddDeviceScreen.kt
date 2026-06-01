package com.gnutux.tahakom.feature.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.irdb.IrDeviceEntry

/**
 * شاشة "إضافة بالاسم/الطراز" — تعرض أجهزة IR الحقيقية من القاعدة (44 جهازاً)،
 * مجمّعة حسب الفئة (TV/Cable/Audio) وقابلة للبحث. النقر على جهاز ينقل لضبطه
 * (اختبار الطاقة/الصوت لتأكيد العلامة). الأجهزة الشبكية تُكتشف تلقائياً في الشاشة الرئيسية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    onBack: () -> Unit,
    onPickIrDevice: (IrDeviceEntry) -> Unit,
    viewModel: AddDeviceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_device_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.add_device_search_ir)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )

            LazyColumn(Modifier.fillMaxSize()) {
                state.irByCategory.forEach { (category, devices) ->
                    item(key = "cat-$category") {
                        Text(
                            text = localizedCategory(category),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(devices, key = { it.file }) { entry ->
                        ElevatedCard(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable { onPickIrDevice(entry) },
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(entry.brand, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(R.string.add_device_buttons_count, entry.buttons),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** اسم الفئة المعرَّب. */
@Composable
private fun localizedCategory(category: String): String = when (category) {
    "TV" -> stringResource(R.string.cat_tv)
    "Cable" -> stringResource(R.string.cat_cable)
    "Audio" -> stringResource(R.string.cat_audio)
    else -> category
}
