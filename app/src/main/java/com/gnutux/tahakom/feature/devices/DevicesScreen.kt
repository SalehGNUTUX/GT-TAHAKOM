package com.gnutux.tahakom.feature.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.gnutux.tahakom.core.discovery.DiscoveredDevice

/**
 * شاشة الأجهزة — تعرض نتائج المسح الحيّ (mDNS/SSDP) أو الحالة الفارغة.
 * المسح أوفلاين بالكامل عبر [DiscoveryViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onOpenSettings: () -> Unit = {},
    onAddManual: () -> Unit = {},
    onDeviceClick: (DiscoveredDevice) -> Unit = {},
    viewModel: DiscoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ScanControls(
                isScanning = state.isScanning,
                onScan = viewModel::startScan,
                onStop = viewModel::stopScan,
                onAddManual = onAddManual,
            )

            if (state.devices.isEmpty()) {
                EmptyState(isScanning = state.isScanning, modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp)) {
                    items(state.devices, key = { it.host + it.transport.name }) { device ->
                        DeviceRow(device, onClick = { onDeviceClick(device) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanControls(
    isScanning: Boolean,
    onScan: () -> Unit,
    onStop: () -> Unit,
    onAddManual: () -> Unit,
) {
    Button(
        onClick = if (isScanning) onStop else onScan,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = 8.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(stringResource(R.string.devices_scanning))
        } else {
            Icon(Icons.Filled.Search, contentDescription = null)
            Text(stringResource(R.string.devices_scan), modifier = Modifier.padding(start = 8.dp))
        }
    }
    OutlinedButton(onClick = onAddManual, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Icon(Icons.Outlined.Add, contentDescription = null)
        Text(stringResource(R.string.devices_add_manual), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun DeviceRow(device: DiscoveredDevice, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(text = device.name, style = MaterialTheme.typography.titleMedium)
            val subtitle = buildString {
                device.brand?.let { append(it) }
                device.model?.let { append(" · ").append(it) }
                if (isNotEmpty()) append(" · ")
                append(device.host)
            }
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${device.transport.name} · ${device.source.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun EmptyState(isScanning: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.SettingsRemote,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.devices_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(
                if (isScanning) R.string.devices_scanning_hint else R.string.devices_empty_subtitle,
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
