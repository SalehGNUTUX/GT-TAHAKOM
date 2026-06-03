package com.gnutux.tahakom.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.settings.AppLanguage
import com.gnutux.tahakom.core.settings.LocaleManager
import com.gnutux.tahakom.ui.icons.TahakomIcon
import com.gnutux.tahakom.ui.theme.tokens

/**
 * شاشة الإعدادات الكاملة (مقتبسة من تصميم SettingsScreen): هوية التطبيق + المظهر
 * (سمة فاتح/داكن/نظام + اللغة) + وسائل الإرسال + حول التطبيق.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    themeMode: String,
    onThemeMode: (String) -> Unit,
    onReplayOnboarding: () -> Unit = {},
    onDeviceReady: (com.gnutux.tahakom.core.model.Device) -> Unit = {},
    addVm: com.gnutux.tahakom.feature.devices.AddDeviceViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val c = tokens.colors
    val context = LocalContext.current
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            com.gnutux.tahakom.core.share.RemotePackSharing.readFromUri(context, uri)?.let { pack ->
                addVm.importPack(pack, onDeviceReady) {}
            }
        }
    }
    val shareText = stringResource(R.string.share_app_text)
    fun shareApp() {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "GT-TAHAKOM — تَحَكُّمْ")
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(android.content.Intent.createChooser(intent, null))
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            // بطاقة هوية التطبيق
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.lg))
                    .background(c.surface).border(1.dp, c.line, RoundedCornerShape(tokens.shape.lg))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("GT-TAHAKOM", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.app_tagline), color = c.textFaint, fontSize = 13.sp)
                }
                Chip("v0.9.16")
            }
            Spacer(Modifier.size(20.dp))

            // قسم المظهر
            SectionTitle(stringResource(R.string.set_appearance))
            SettingRow("moon", stringResource(R.string.set_theme)) {
                Segmented(
                    options = listOf(
                        "system" to stringResource(R.string.theme_system),
                        "light" to stringResource(R.string.set_light),
                        "dark" to stringResource(R.string.set_dark),
                    ),
                    selected = themeMode,
                    onSelect = onThemeMode,
                )
            }
            SettingRow("globe", stringResource(R.string.settings_language), last = true) {
                var current = LocaleManager.current()
                Segmented(
                    options = listOf(
                        AppLanguage.SYSTEM.name to stringResource(R.string.language_system),
                        AppLanguage.ARABIC.name to "ع",
                        AppLanguage.ENGLISH.name to "EN",
                    ),
                    selected = current.name,
                    onSelect = { LocaleManager.apply(AppLanguage.valueOf(it)) },
                )
            }
            Spacer(Modifier.size(20.dp))

            // قسم وسائل الإرسال (معلومات)
            SectionTitle(stringResource(R.string.set_transports))
            InfoRow("wifi", stringResource(R.string.set_network), stringResource(R.string.set_network_sub))
            InfoRow("ir", stringResource(R.string.set_ir), stringResource(R.string.set_ir_sub))
            InfoRow("bridge", stringResource(R.string.set_bridge), stringResource(R.string.set_bridge_sub), last = true)
            Spacer(Modifier.size(20.dp))

            // قسم حول
            SectionTitle(stringResource(R.string.set_about))
            InfoRow("info", stringResource(R.string.set_version), "0.9.16")
            InfoRow("gear", stringResource(R.string.set_developer), stringResource(R.string.set_developer_name))
            val uriHandler = LocalUriHandler.current
            InfoRow(
                "link", stringResource(R.string.set_repo), stringResource(R.string.set_repo_url),
                onClick = { uriHandler.openUri("https://github.com/SalehGNUTUX/GT-TAHAKOM") },
            )
            InfoRow("shield", stringResource(R.string.set_license), "GPLv3", last = true)
            Spacer(Modifier.size(20.dp))

            // إجراءات: مشاركة التطبيق + استيراد لوحة تحكّم.
            SectionTitle(stringResource(R.string.set_actions))
            ActionRow("share", stringResource(R.string.set_share_app)) { shareApp() }
            ActionRow("link", stringResource(R.string.set_import_remote)) {
                importLauncher.launch(arrayOf("*/*"))
            }
            ActionRow("homeBtn", stringResource(R.string.set_replay_onboarding), last = true) { onReplayOnboarding() }
            Spacer(Modifier.size(12.dp))
        }
    }
}

/** صف إجراء قابل للنقر بأيقونة + سهم. */
@Composable
private fun ActionRow(icon: String, label: String, last: Boolean = false, onClick: () -> Unit) {
    val c = tokens.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon)
        Text(label, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 12.dp))
        TahakomIcon("forwardNav", c.textFaint, size = 18.dp)
    }
    if (!last) Divider()
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(), color = tokens.colors.textFaint, fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(icon: String, label: String, last: Boolean = false, trailing: @Composable () -> Unit) {
    val c = tokens.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon)
        Text(label, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 12.dp))
        trailing()
    }
    if (!last) Divider()
}

@Composable
private fun InfoRow(icon: String, label: String, value: String, last: Boolean = false, onClick: (() -> Unit)? = null) {
    val c = tokens.colors
    // إن كانت القيمة طويلة (مثل رابط) نضعها سطراً تحت العنوان بدل مزاحمته أفقياً.
    val stacked = value.length > 18
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon)
        if (stacked) {
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(label, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = c.textFaint, fontSize = 12.5.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp))
            }
        } else {
            Text(label, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 12.dp))
            Text(value, color = c.textFaint, fontSize = 13.sp, maxLines = 1)
        }
    }
    if (!last) Divider()
}

@Composable
private fun IconBox(icon: String) {
    val c = tokens.colors
    Box(
        Modifier.size(38.dp).clip(RoundedCornerShape(tokens.shape.sm)).background(c.bg2),
        contentAlignment = Alignment.Center,
    ) { TahakomIcon(icon, c.textDim, size = 20.dp) }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().size(1.dp).background(tokens.colors.line))
}

@Composable
private fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val c = tokens.colors
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(c.bg2).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (active) c.surface else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    label,
                    color = if (active) c.text else c.textFaint,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    val c = tokens.colors
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(c.accentSoft).padding(horizontal = 11.dp, vertical = 5.dp),
    ) { Text(text, color = c.accent, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) }
}
