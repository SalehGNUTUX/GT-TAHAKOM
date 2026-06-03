package com.gnutux.tahakom.feature.devices

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.style.TextAlign
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
 * شاشة "إضافة بالاسم/الطراز". تنظيم بمستويين لتوفير المساحة:
 * - **الصفحة الرئيسية:** صفّ إجراءات مضغوط (إنترنت/شبكي/تعلّم/ملف) + بحث عام يعرض نتائج
 *   من كل الفئات عند الكتابة، أو بطاقات الفئات (تلفاز/استقبال/صوت) عند فراغه.
 * - **داخل فئة:** قائمة أجهزة الفئة بكامل الشاشة + بحث مخصّص للفئة + رجوع.
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
    // فئة مفتوحة (عرض بكامل الشاشة) أو null = الصفحة الرئيسية.
    var openCategory by remember { mutableStateOf<String?>(null) }
    var categoryQuery by remember { mutableStateOf("") }

    // زر الرجوع (الجهاز) يغلق الفئة أولاً قبل مغادرة الشاشة.
    BackHandler(enabled = openCategory != null) { openCategory = null; categoryQuery = "" }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val pack = RemotePackSharing.readFromUri(context, uri)
        if (pack == null) { importError = true; return@rememberLauncherForActivityResult }
        viewModel.importPack(pack, onDeviceReady) { importError = true }
    }

    val cat = openCategory
    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (cat != null) localizedCategory(cat) else stringResource(R.string.add_device_title),
                        color = c.text, fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg),
                navigationIcon = {
                    Box(
                        Modifier.padding(start = 8.dp).size(40.dp).clip(RoundedCornerShape(tokens.shape.sm))
                            .background(c.surface)
                            .clickable { if (cat != null) { openCategory = null; categoryQuery = "" } else onBack() },
                        contentAlignment = Alignment.Center,
                    ) { TahakomIcon("back", c.textDim, size = 20.dp) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (cat == null) {
                LandingContent(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    importError = importError,
                    onSearchOnline = onSearchOnline,
                    onAddNetwork = onAddNetwork,
                    onLearn = onLearn,
                    onImport = { importLauncher.launch(arrayOf("*/*")) },
                    onPickIrDevice = onPickIrDevice,
                    onOpenCategory = { openCategory = it; categoryQuery = "" },
                )
            } else {
                CategoryContent(
                    category = cat,
                    devices = state.irByCategory[cat].orEmpty(),
                    query = categoryQuery,
                    onQueryChange = { categoryQuery = it },
                    onPickIrDevice = onPickIrDevice,
                )
            }
        }
    }
}

@Composable
private fun LandingContent(
    state: AddDeviceUiState,
    onQueryChange: (String) -> Unit,
    importError: Boolean,
    onSearchOnline: () -> Unit,
    onAddNetwork: () -> Unit,
    onLearn: () -> Unit,
    onImport: () -> Unit,
    onPickIrDevice: (IrDeviceEntry) -> Unit,
    onOpenCategory: (String) -> Unit,
) {
    val c = tokens.colors
    // صفّ إجراءات مضغوط — لا يزاحم القائمة.
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompactAction("scan", stringResource(R.string.add_act_online), Modifier.weight(1f), onSearchOnline)
        CompactAction("wifi", stringResource(R.string.add_act_network), Modifier.weight(1f), onAddNetwork)
        CompactAction("plus", stringResource(R.string.add_act_learn), Modifier.weight(1f), onLearn)
        CompactAction("source", stringResource(R.string.add_act_import), Modifier.weight(1f), onImport)
    }
    if (importError) {
        Text(stringResource(R.string.import_error), color = c.ir, fontSize = 12.5.sp,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp))
    }
    SearchField(
        value = state.query,
        onValueChange = onQueryChange,
        label = stringResource(R.string.add_device_search_ir),
    )
    Spacer(Modifier.size(10.dp))

    if (state.query.isBlank()) {
        // بطاقات الفئات (الصفحة الرئيسية فارغة البحث).
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.irByCategory.toList(), key = { it.first }) { (category, devices) ->
                CategoryCard(category, devices.size) { onOpenCategory(category) }
            }
            item(key = "tail") { Spacer(Modifier.size(8.dp)) }
        }
    } else {
        // نتائج البحث العام عبر كل الفئات.
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.irByCategory.forEach { (category, devices) ->
                item(key = "h-$category") { SectionHeader(localizedCategory(category)) }
                items(devices, key = { it.file }) { entry ->
                    IrDeviceCard(category, entry) { onPickIrDevice(entry) }
                }
            }
            item(key = "tail") { Spacer(Modifier.size(8.dp)) }
        }
    }
}

@Composable
private fun CategoryContent(
    category: String,
    devices: List<IrDeviceEntry>,
    query: String,
    onQueryChange: (String) -> Unit,
    onPickIrDevice: (IrDeviceEntry) -> Unit,
) {
    // بحث مخصّص للفئة فقط.
    val q = query.trim().lowercase()
    val filtered = if (q.isEmpty()) devices
    else devices.filter { it.brand.lowercase().contains(q) || it.model.lowercase().contains(q) }

    SearchField(
        value = query,
        onValueChange = onQueryChange,
        label = stringResource(R.string.add_search_in, localizedCategory(category)),
    )
    Spacer(Modifier.size(10.dp))
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filtered, key = { it.file }) { entry ->
            IrDeviceCard(category, entry) { onPickIrDevice(entry) }
        }
        item(key = "tail") { Spacer(Modifier.size(8.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, label: String) {
    val c = tokens.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { TahakomIcon("search", c.textFaint, size = 20.dp) },
        singleLine = true,
        shape = RoundedCornerShape(tokens.shape.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.accent, unfocusedBorderColor = c.line,
            focusedTextColor = c.text, unfocusedTextColor = c.text,
            focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
            focusedLabelColor = c.accent, unfocusedLabelColor = c.textFaint, cursorColor = c.accent,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
}

/** زر إجراء مضغوط: صندوق أيقونة + تسمية قصيرة تحته. */
@Composable
private fun CompactAction(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    val c = tokens.colors
    Column(
        modifier.clip(RoundedCornerShape(tokens.shape.md)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.md))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TahakomIcon(icon, c.accent, size = 22.dp)
        Text(label, color = c.textDim, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

/** بطاقة فئة (تلفاز/استقبال/صوت) + عدد الأجهزة + سهم. */
@Composable
private fun CategoryCard(category: String, count: Int, onClick: () -> Unit) {
    val c = tokens.colors
    val icon = categoryIcon(category)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.lg)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.lg))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(tokens.shape.sm)).background(c.accentSoft),
            contentAlignment = Alignment.Center,
        ) { TahakomIcon(icon, c.accent, size = 24.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(localizedCategory(category), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.cat_device_count, count), color = c.textFaint, fontSize = 12.5.sp)
        }
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

/** بطاقة جهاز IR: أيقونة الفئة + العلامة + عدد الأزرار + سهم. */
@Composable
private fun IrDeviceCard(category: String, entry: IrDeviceEntry, onClick: () -> Unit) {
    val c = tokens.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.lg)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.lg))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(tokens.shape.sm)).background(c.irSoft),
            contentAlignment = Alignment.Center,
        ) { TahakomIcon(categoryIcon(category), c.ir, size = 22.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(entry.brand, color = c.text, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.add_device_buttons_count, entry.buttons),
                color = c.textFaint, fontSize = 12.5.sp)
        }
        TahakomIcon("forwardNav", c.textFaint, size = 18.dp)
    }
}

private fun categoryIcon(category: String): String = when (category) {
    "TV" -> "tv"
    "Cable" -> "source"
    "Audio" -> "volUp"
    else -> "ir"
}

/** اسم الفئة المعرَّب. */
@Composable
private fun localizedCategory(category: String): String = when (category) {
    "TV" -> stringResource(R.string.cat_tv)
    "Cable" -> stringResource(R.string.cat_cable)
    "Audio" -> stringResource(R.string.cat_audio)
    else -> category
}
