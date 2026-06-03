package com.gnutux.tahakom.feature.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.discovery.toDevice
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.model.DeviceType
import com.gnutux.tahakom.core.transport.TransportType
import com.gnutux.tahakom.ui.icons.TahakomIcon
import com.gnutux.tahakom.ui.theme.tokens

/** بروتوكولات الشبكة التي يمكن إضافتها يدوياً بالـ IP (المنفّذة فعلاً). */
private val NETWORK_OPTIONS = listOf(
    Triple(TransportType.LG_WEBOS, "LG (webOS)", "wifi"),
    Triple(TransportType.SAMSUNG_TIZEN, "Samsung (Tizen)", "wifi"),
    Triple(TransportType.ROKU, "Roku", "wifi"),
    Triple(TransportType.ANDROID_TV, "Android TV", "wifi"),
)

/**
 * إضافة جهاز شبكي يدوياً بعنوان IP (حين لا يظهر في المسح التلقائي).
 * يختار المستخدم البروتوكول ويُدخل الـ IP فيُنشأ [Device] شبكي ويُفتح ريموته.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNetworkScreen(
    onBack: () -> Unit,
    onDeviceReady: (Device) -> Unit,
    scanVm: DevicesViewModel = hiltViewModel(),
) {
    val c = tokens.colors
    var selected by remember { mutableStateOf(TransportType.LG_WEBOS) }
    var ip by remember { mutableStateOf("") }
    val ipValid = ip.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))

    var scanning by remember { mutableStateOf(false) }
    val discovery by scanVm.discovery.collectAsStateWithLifecycle()
    // مسح مخصّص للنوع المحدّد: نعرض المكتشَف المطابق لوسيلته فقط.
    val matches = discovery.discovered.filter { it.transport == selected }
    DisposableEffect(scanning) {
        if (scanning) scanVm.startScan() else scanVm.stopScan()
        onDispose { scanVm.stopScan() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.net_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.net_hint),
                color = c.textDim, fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            Text(stringResource(R.string.net_protocol), color = c.textFaint, fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            NETWORK_OPTIONS.forEach { (type, label, _) ->
                val active = type == selected
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(tokens.shape.md))
                        .background(if (active) c.accentSoft else c.surface)
                        .border(1.5.dp, if (active) c.accent else c.line, RoundedCornerShape(tokens.shape.md))
                        .clickable { selected = type }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(11.dp).clip(RoundedCornerShape(50)).background(c.wifi))
                    // Android TV تجريبي → نُلحق وسم «تجريبي» المترجَم بالعلامة.
                    val shown = if (type == TransportType.ANDROID_TV) "$label (${stringResource(R.string.experimental)})" else label
                    Text(shown, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 12.dp))
                    if (active) Text("✓", color = c.accent, fontWeight = FontWeight.Bold)
                }
            }

            // مسح مخصّص للنوع المحدّد على الشبكة (قبل إدخال IP يدوياً).
            val selectedLabel = NETWORK_OPTIONS.first { it.first == selected }.second
            Spacer(Modifier.size(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.md)).background(c.surface)
                    .border(1.dp, c.line, RoundedCornerShape(tokens.shape.md))
                    .clickable { scanning = !scanning }.padding(vertical = 13.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (scanning) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = c.accent)
                else TahakomIcon("scan", c.accent, size = 20.dp)
                Text(
                    if (scanning) stringResource(R.string.net_scanning) else stringResource(R.string.net_scan_for, selectedLabel),
                    color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
            }
            // الأجهزة المكتشَفة المطابقة للنوع — النقر يفتح ريموتها مباشرة.
            matches.forEach { d ->
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(tokens.shape.md))
                        .background(c.accentSoft).clickable { onDeviceReady(d.toDevice()) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TahakomIcon("wifi", c.accent, size = 20.dp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(d.name, color = c.text, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(d.host, color = c.textFaint, fontSize = 12.sp)
                    }
                    TahakomIcon("plus", c.accent, size = 18.dp)
                }
            }

            Spacer(Modifier.size(16.dp))
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it.trim() },
                label = { Text(stringResource(R.string.net_ip_label)) },
                placeholder = { Text("192.168.1.20") },
                singleLine = true,
                // عنوان IP أرقام ونقاط فقط → لوحة رقمية عشرية (تتضمّن مفتاح النقطة) لا حرفية.
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )

            Spacer(Modifier.size(20.dp))
            Button(
                onClick = {
                    val label = NETWORK_OPTIONS.first { it.first == selected }.second
                    onDeviceReady(
                        Device(
                            id = "net-${selected.name}-$ip",
                            name = label,
                            type = DeviceType.TV,
                            transport = selected,
                            address = ip,
                            metadata = mapOf("brand" to label),
                        ),
                    )
                },
                enabled = ipValid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.net_connect)) }

            Text(
                stringResource(R.string.net_pairing_note),
                color = c.textFaint, fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
