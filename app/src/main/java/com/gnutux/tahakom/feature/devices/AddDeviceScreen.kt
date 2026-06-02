package com.gnutux.tahakom.feature.devices

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.irdb.IrDeviceEntry
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.share.RemotePackSharing
import com.gnutux.tahakom.ui.icons.TahakomIcon
import com.gnutux.tahakom.ui.theme.tokens

/**
 * شاشة "إضافة بالاسم/الطراز" — تعرض أجهزة IR الحقيقية من القاعدة، مجمّعة حسب الفئة
 * (TV/Cable/Audio) وقابلة للبحث. النقر على جهاز ينقل لضبطه (اختبار الطاقة/الصوت لتأكيد
 * العلامة). الأجهزة الشبكية تُكتشف تلقائياً في الشاشة الرئيسية. بسمة serene عبر `tokens`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    onBack: () -> Unit,
    onPickIrDevice: (IrDeviceEntry) -> Unit,
    onLearn: () -> Unit = {},
    onAddNetwork: () -> Unit = {},
    onSearchOnline: () -> Unit = {},
    onDeviceReady: (Device) -> Unit = {},
    viewModel: AddDeviceViewModel = hiltViewModel(),
) {
    val c = tokens.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var importError by remember { mutableStateOf(false) }

    // منتقي ملف .tahakom: يقرأ الحزمة، فإن طابقت جهازاً في القاعدة اعتُمد مباشرة،
    // وإلا أُنشئ جهاز من معلومات الحزمة (الاسم/الفئة).
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val pack = RemotePackSharing.readFromUri(context, uri)
        if (pack == null) { importError = true; return@rememberLauncherForActivityResult }
        viewModel.importPack(pack, onDeviceReady) { importError = true }
    }

    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_device_title), color = c.text, fontWeight = FontWeight.Bold) },
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
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.add_device_search_ir)) },
                leadingIcon = { TahakomIcon("search", c.textFaint, size = 20.dp) },
                singleLine = true,
                shape = RoundedCornerShape(tokens.shape.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.line,
                    focusedTextColor = c.text,
                    unfocusedTextColor = c.text,
                    focusedContainerColor = c.surface,
                    unfocusedContainerColor = c.surface,
                    focusedLabelColor = c.accent,
                    unfocusedLabelColor = c.textFaint,
                    cursorColor = c.accent,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            Spacer(Modifier.size(10.dp))
            // مداخل بديلة: بحث شبكي · جهاز شبكي بالـ IP · تعلّم يدوي · استيراد ملف.
            SecondaryAction("scan", stringResource(R.string.online_search_entry), onSearchOnline)
            Spacer(Modifier.size(8.dp))
            SecondaryAction("wifi", stringResource(R.string.net_entry), onAddNetwork)
            Spacer(Modifier.size(8.dp))
            SecondaryAction("plus", stringResource(R.string.learn_entry), onLearn)
            Spacer(Modifier.size(8.dp))
            SecondaryAction("source", stringResource(R.string.import_file)) { importLauncher.launch(arrayOf("*/*")) }
            if (importError) {
                Text(
                    stringResource(R.string.import_error), color = c.ir, fontSize = 12.5.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }

            LazyColumn(
                Modifier.fillMaxSize().padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.irByCategory.forEach { (category, devices) ->
                    item(key = "cat-$category") { SectionHeader(localizedCategory(category)) }
                    items(devices, key = { it.file }) { entry ->
                        IrDeviceCard(category = category, entry = entry, onClick = { onPickIrDevice(entry) })
                    }
                }
                item(key = "tail") { Spacer(Modifier.size(8.dp)) }
            }
        }
    }
}

/** زر إجراء ثانوي (سطح + حدّ + أيقونة الإبراز). */
@Composable
private fun SecondaryAction(icon: String, label: String, onClick: () -> Unit) {
    val c = tokens.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.md)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.md))
            .clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TahakomIcon(icon, c.accent, size = 20.dp)
        Text(label, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 12.dp))
        TahakomIcon("forwardNav", c.textFaint, size = 18.dp)
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

/** بطاقة جهاز IR من القاعدة: أيقونة الفئة + العلامة + عدد الأزرار + سهم. */
@Composable
private fun IrDeviceCard(category: String, entry: IrDeviceEntry, onClick: () -> Unit) {
    val c = tokens.colors
    val icon = when (category) {
        "TV" -> "tv"
        "Cable" -> "source"
        "Audio" -> "volUp"
        else -> "ir"
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.lg)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.lg))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(tokens.shape.sm)).background(c.irSoft),
            contentAlignment = Alignment.Center,
        ) { TahakomIcon(icon, c.ir, size = 22.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(entry.brand, color = c.text, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.add_device_buttons_count, entry.buttons),
                color = c.textFaint, fontSize = 12.5.sp,
            )
        }
        TahakomIcon("forwardNav", c.textFaint, size = 18.dp)
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
