package com.gnutux.tahakom.feature.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.discovery.DiscoveredDevice
import com.gnutux.tahakom.core.model.Device
import kotlinx.coroutines.launch

/**
 * الشاشة الرئيسية: قائمة الأجهزة المحفوظة (فتح/مشاركة/حذف بتأكيد وتراجع/إعادة ترتيب)
 * + قسم المسح الحيّ لاكتشاف أجهزة جديدة على الشبكة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onOpenSettings: () -> Unit = {},
    onAddManual: () -> Unit = {},
    onOpenDevice: (Device) -> Unit = {},
    onAdoptDiscovered: (DiscoveredDevice) -> Unit = {},
    onShareDevice: (Device) -> Unit = {},
    viewModel: DevicesViewModel = hiltViewModel(),
) {
    val saved by viewModel.savedDevices.collectAsStateWithLifecycle()
    val discovery by viewModel.discovery.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Device?>(null) }

    val undoLabel = stringResource(R.string.action_undo)
    val deletedMsg = stringResource(R.string.device_deleted)

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
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(
                onClick = if (discovery.isScanning) viewModel::stopScan else viewModel::startScan,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (discovery.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp,
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

            if (saved.isEmpty() && discovery.discovered.isEmpty()) {
                EmptyState(isScanning = discovery.isScanning, modifier = Modifier.fillMaxSize())
            } else {
                val savedIds = saved.map { it.id }.toSet()
                val fresh = discovery.discovered.filter { "${it.host}:${it.port}" !in savedIds }
                LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp)) {
                    // نتائج البحث (المكتشَفة حديثاً) تظهر في الأعلى.
                    if (fresh.isNotEmpty()) {
                        item(key = "h-disc") { SectionHeader(stringResource(R.string.devices_discovered)) }
                        items(fresh, key = { "d-${it.host}-${it.transport.name}" }) { device ->
                            DiscoveredRow(device, onClick = { onAdoptDiscovered(device) })
                        }
                    }
                    if (saved.isNotEmpty()) {
                        item(key = "h-saved") { SectionHeader(stringResource(R.string.devices_my)) }
                        items(saved, key = { "s-${it.id}" }) { device ->
                            val index = saved.indexOf(device)
                            SavedDeviceRow(
                                device = device,
                                canMoveUp = index > 0,
                                canMoveDown = index < saved.lastIndex,
                                onOpen = { onOpenDevice(device) },
                                onShare = { onShareDevice(device) },
                                onDelete = { pendingDelete = device },
                                onMoveUp = { viewModel.move(index, index - 1) },
                                onMoveDown = { viewModel.move(index, index + 1) },
                            )
                        }
                    }
                }
            }
        }
    }

    // حوار تأكيد الحذف + تراجع 5 ثوانٍ بعده.
    pendingDelete?.let { device ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.device_delete_title)) },
            text = { Text(stringResource(R.string.device_delete_confirm, device.name)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.remove(device.id)
                    scope.launch {
                        val result = snackbar.showSnackbar(
                            message = deletedMsg, actionLabel = undoLabel, duration = androidx.compose.material3.SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.save(device)
                    }
                }) { Text(stringResource(R.string.device_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text, style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SavedDeviceRow(
    device: Device,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onOpen)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            // مقابض إعادة الترتيب
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                }
            }
            Column(Modifier.weight(1f).padding(vertical = 16.dp, horizontal = 8.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${device.metadata["brand"] ?: device.transport.name}" +
                        (device.address?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share_pack))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.device_delete))
            }
        }
    }
}

@Composable
private fun DiscoveredRow(device: DiscoveredDevice, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium)
            val subtitle = buildString {
                device.brand?.let { append(it).append(" · ") }
                append(device.host)
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            Text(
                "${device.transport.name} · ${device.source.name}",
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
            imageVector = Icons.Outlined.SettingsRemote, contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp), tint = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.devices_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(if (isScanning) R.string.devices_scanning_hint else R.string.devices_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
