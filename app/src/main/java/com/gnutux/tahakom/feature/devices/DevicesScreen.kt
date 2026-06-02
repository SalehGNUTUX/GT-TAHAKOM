package com.gnutux.tahakom.feature.devices

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.discovery.DiscoveredDevice
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.transport.TransportType
import com.gnutux.tahakom.ui.icons.TahakomIcon
import com.gnutux.tahakom.ui.theme.tokens
import kotlinx.coroutines.launch

/**
 * الشاشة الرئيسية: قائمة الأجهزة المحفوظة (فتح/مشاركة/حذف بتأكيد وتراجع/إعادة ترتيب)
 * + قسم المسح الحيّ لاكتشاف أجهزة جديدة على الشبكة. بسمة serene عبر `tokens`.
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
    val c = tokens.colors
    val saved by viewModel.savedDevices.collectAsStateWithLifecycle()
    val discovery by viewModel.discovery.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Device?>(null) }

    val undoLabel = stringResource(R.string.action_undo)
    val deletedMsg = stringResource(R.string.device_deleted)

    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), color = c.text, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg),
                actions = {
                    Box(
                        Modifier.padding(end = 8.dp).size(40.dp).clip(RoundedCornerShape(tokens.shape.sm))
                            .background(c.surface).clickable(onClick = onOpenSettings),
                        contentAlignment = Alignment.Center,
                    ) { TahakomIcon("gear", c.textDim, size = 20.dp) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // إجراء المسح الأساسي (بارز بلون الإبراز) + إضافة يدوية ثانوية.
            PrimaryAction(
                label = if (discovery.isScanning) stringResource(R.string.devices_scanning)
                else stringResource(R.string.devices_scan),
                scanning = discovery.isScanning,
                onClick = if (discovery.isScanning) viewModel::stopScan else viewModel::startScan,
            )
            Spacer(Modifier.size(8.dp))
            SecondaryAction(
                icon = "plus",
                label = stringResource(R.string.devices_add_manual),
                onClick = onAddManual,
            )

            if (saved.isEmpty() && discovery.discovered.isEmpty()) {
                EmptyState(isScanning = discovery.isScanning, modifier = Modifier.fillMaxSize())
            } else {
                val savedIds = saved.map { it.id }.toSet()
                val fresh = discovery.discovered.filter { "${it.host}:${it.port}" !in savedIds }
                LazyColumn(
                    Modifier.fillMaxSize().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                    item(key = "tail") { Spacer(Modifier.size(8.dp)) }
                }
            }
        }
    }

    // حوار تأكيد الحذف + تراجع 5 ثوانٍ بعده.
    pendingDelete?.let { device ->
        AlertDialog(
            containerColor = c.surface,
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.device_delete_title), color = c.text) },
            text = { Text(stringResource(R.string.device_delete_confirm, device.name), color = c.textDim) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.remove(device.id)
                    scope.launch {
                        val result = snackbar.showSnackbar(
                            message = deletedMsg, actionLabel = undoLabel,
                            duration = androidx.compose.material3.SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.save(device)
                    }
                }) { Text(stringResource(R.string.device_delete), color = c.ir) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel), color = c.textDim)
                }
            },
        )
    }
}

/** زر إجراء أساسي بلون الإبراز (المسح). */
@Composable
private fun PrimaryAction(label: String, scanning: Boolean, onClick: () -> Unit) {
    val c = tokens.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.md)).background(c.accent)
            .clickable(onClick = onClick).padding(vertical = 16.dp, horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (scanning) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp, color = c.accentText)
        } else {
            TahakomIcon("scan", c.accentText, size = 20.dp)
        }
        Text(label, color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 10.dp))
    }
}

/** زر إجراء ثانوي (سطح + حدّ). */
@Composable
private fun SecondaryAction(icon: String, label: String, onClick: () -> Unit) {
    val c = tokens.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.md)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.md))
            .clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TahakomIcon(icon, c.accent, size = 20.dp)
        Text(label, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(), color = tokens.colors.textFaint, fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 2.dp),
    )
}

/** صندوق أيقونة الوسيلة بلونها الناعم (WiFi/IR/جسر). */
@Composable
private fun TransportIconBox(transport: TransportType) {
    val c = tokens.colors
    val (icon, color, soft) = when (transport) {
        TransportType.IR -> Triple("ir", c.ir, c.irSoft)
        TransportType.BROADLINK -> Triple("bridge", c.bridge, c.bridgeSoft)
        else -> Triple("wifi", c.wifi, c.wifiSoft)
    }
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(tokens.shape.sm)).background(soft),
        contentAlignment = Alignment.Center,
    ) { TahakomIcon(icon, color, size = 22.dp) }
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
    val c = tokens.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.lg)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.lg))
            .clickable(onClick = onOpen).padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // مقابض إعادة الترتيب
        Column {
            ReorderHandle("caretUp", enabled = canMoveUp, desc = stringResource(R.string.move_up), onClick = onMoveUp)
            ReorderHandle("caretDown", enabled = canMoveDown, desc = stringResource(R.string.move_down), onClick = onMoveDown)
        }
        TransportIconBox(device.transport)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(device.name, color = c.text, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${device.metadata["brand"] ?: device.transport.name}" +
                    (device.address?.let { " · $it" } ?: ""),
                color = c.textFaint, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        ActionIcon("share", c.textDim, stringResource(R.string.share_pack), onShare)
        ActionIcon("delete", c.ir, stringResource(R.string.device_delete), onDelete)
    }
}

@Composable
private fun ReorderHandle(icon: String, enabled: Boolean, desc: String, onClick: () -> Unit) {
    val c = tokens.colors
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { TahakomIcon(icon, if (enabled) c.textDim else c.line, size = 20.dp) }
}

@Composable
private fun ActionIcon(icon: String, tint: Color, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(tokens.shape.sm)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { TahakomIcon(icon, tint, size = 20.dp) }
}

@Composable
private fun DiscoveredRow(device: DiscoveredDevice, onClick: () -> Unit) {
    val c = tokens.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.lg)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.lg))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportIconBox(device.transport)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(device.name, color = c.text, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = buildString {
                device.brand?.let { append(it).append(" · ") }
                append(device.host)
            }
            Text(subtitle, color = c.textFaint, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // شارة "إضافة" تلمّح للنقر للاعتماد.
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(c.accentSoft)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) { TahakomIcon("plus", c.accent, size = 18.dp) }
    }
}

@Composable
private fun EmptyState(isScanning: Boolean, modifier: Modifier = Modifier) {
    val c = tokens.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(88.dp).clip(RoundedCornerShape(24.dp)).background(c.accentSoft),
            contentAlignment = Alignment.Center,
        ) { TahakomIcon("ir", c.accent, size = 40.dp) }
        Spacer(Modifier.size(18.dp))
        Text(stringResource(R.string.devices_empty_title), color = c.text, fontSize = 19.sp,
            fontWeight = FontWeight.Bold)
        Text(
            stringResource(if (isScanning) R.string.devices_scanning_hint else R.string.devices_empty_subtitle),
            color = c.textFaint, fontSize = 13.5.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
        )
    }
}
