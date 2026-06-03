package com.gnutux.tahakom.feature.devices

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.gnutux.tahakom.ui.icons.TahakomIcon
import com.gnutux.tahakom.ui.theme.tokens
import kotlinx.coroutines.launch

/**
 * صفحة المسح المستقلّة: رادار متذبذب + «يبحث في محيطك…»، تظهر النتائج حيّةً عند اكتشافها،
 * زر إيقاف/إعادة، والنقر على «+» يضيف الجهاز إلى «أجهزتي». المسح يبدأ عند الدخول ويتوقّف
 * عند المغادرة. يفصل نتائج الاكتشاف عن قائمة أجهزتي لتنظيم أوضح.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onAdopt: (DiscoveredDevice) -> Unit,
    viewModel: DevicesViewModel = hiltViewModel(),
) {
    val c = tokens.colors
    val saved by viewModel.savedDevices.collectAsStateWithLifecycle()
    val discovery by viewModel.discovery.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedFmt = stringResource(R.string.device_added)

    // يبدأ المسح تلقائياً عند فتح الصفحة، ويتوقّف عند مغادرتها.
    DisposableEffect(Unit) {
        viewModel.startScan()
        onDispose { viewModel.stopScan() }
    }

    val savedIds = saved.map { it.id }.toSet()
    val fresh = discovery.discovered.filter { "${it.host}:${it.port}" !in savedIds }

    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_title), color = c.text, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg),
                navigationIcon = {
                    Box(
                        Modifier.padding(start = 8.dp).size(40.dp).clip(RoundedCornerShape(tokens.shape.sm))
                            .background(c.surface).clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) { TahakomIcon("back", c.textDim, size = 20.dp) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Radar(scanning = discovery.isScanning)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(if (discovery.isScanning) R.string.scan_searching else R.string.scan_again),
                color = c.textDim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            // زر إيقاف/إعادة البحث.
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                    .background(if (discovery.isScanning) c.surface else c.accent)
                    .border(if (discovery.isScanning) 1.dp else 0.dp, c.line, RoundedCornerShape(50))
                    .clickable { if (discovery.isScanning) viewModel.stopScan() else viewModel.startScan() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TahakomIcon(
                        if (discovery.isScanning) "close" else "scan",
                        if (discovery.isScanning) c.text else c.accentText, size = 18.dp,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        stringResource(if (discovery.isScanning) R.string.scan_stop else R.string.scan_again),
                        color = if (discovery.isScanning) c.text else c.accentText,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            if (fresh.isEmpty()) {
                Text(
                    stringResource(R.string.scan_none), color = c.textFaint, fontSize = 13.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            } else {
                Text(
                    stringResource(R.string.scan_found).uppercase(), color = c.textFaint, fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                )
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(fresh, key = { "${it.host}-${it.transport.name}" }) { device ->
                        DiscoveredCard(device) {
                            onAdopt(device)
                            scope.launch { snackbar.showSnackbar(addedFmt.replace("%1\$s", device.name)) }
                        }
                    }
                    item(key = "tail") { Spacer(Modifier.size(8.dp)) }
                }
            }
        }
    }
}

/** رادار متذبذب: حلقات تتوسّع وتتلاشى أثناء المسح + أيقونة وسط. */
@Composable
private fun Radar(scanning: Boolean) {
    val c = tokens.colors
    val transition = rememberInfiniteTransition(label = "radar")
    val progress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "p",
    )
    Box(
        Modifier.fillMaxWidth().height(190.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (scanning) {
            Canvas(Modifier.size(180.dp)) {
                val maxR = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                // ثلاث حلقات بأطوار متباعدة لإيحاء الذبذبة المستمرة.
                for (i in 0 until 3) {
                    val phase = (progress + i / 3f) % 1f
                    drawCircle(
                        color = c.accent.copy(alpha = (1f - phase) * 0.55f),
                        radius = phase * maxR, center = center, style = Stroke(width = 3f),
                    )
                }
            }
        }
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(c.accentSoft),
            contentAlignment = Alignment.Center,
        ) { TahakomIcon("wifi", c.accent, size = 34.dp) }
    }
}

@Composable
private fun DiscoveredCard(device: DiscoveredDevice, onAdd: () -> Unit) {
    val c = tokens.colors
    val (icon, color, soft) = when (device.transport.name) {
        "IR" -> Triple("ir", c.ir, c.irSoft)
        "BROADLINK" -> Triple("bridge", c.bridge, c.bridgeSoft)
        else -> Triple("wifi", c.wifi, c.wifiSoft)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.lg)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.lg))
            .clickable(onClick = onAdd).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(tokens.shape.sm)).background(soft),
            contentAlignment = Alignment.Center,
        ) { TahakomIcon(icon, color, size = 22.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(device.name, color = c.text, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = buildString {
                device.brand?.let { append(it).append(" · ") }
                append(device.host)
            }
            Text(subtitle, color = c.textFaint, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(c.accentSoft).padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) { TahakomIcon("plus", c.accent, size = 18.dp) }
    }
}
