package com.gnutux.tahakom.feature.online

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.irdb.online.OnlineEntry
import com.gnutux.tahakom.ui.icons.TahakomIcon
import com.gnutux.tahakom.ui.theme.tokens

/**
 * البحث الشبكي عن أجهزة التحكّم في قاعدة probonopd/irdb. البحث محلي (فهرس مشحون)؛ تنزيل
 * أكواد الطقم المختار يحتاج إنترنت ثم يُحوَّل على الهاتف ويُحفظ، فيُفتح في شاشة الضبط.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchScreen(
    onBack: () -> Unit,
    onOpenSetup: (String) -> Unit,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val c = tokens.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val event by viewModel.events.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var catFilter by remember { mutableStateOf<String?>(null) } // null=الكل · TV/Cable/Audio

    val savedMsg = stringResource(R.string.online_saved, "%s")
    val unsupportedFmt = stringResource(R.string.online_unsupported_msg, "%s")
    val errorFmt = stringResource(R.string.online_fetch_error, "%s")

    // معالجة الأحداث: نجاح → فتح الضبط؛ غير مدعوم/خطأ → إشعار.
    LaunchedEffect(event) {
        when (val e = event) {
            is OnlineEvent.Saved -> { viewModel.consumeEvent(); onOpenSetup(e.file) }
            is OnlineEvent.Unsupported -> {
                snackbar.showSnackbar(unsupportedFmt.replace("%s", e.protocol)); viewModel.consumeEvent()
            }
            is OnlineEvent.Error -> {
                snackbar.showSnackbar(errorFmt.replace("%s", e.reason)); viewModel.consumeEvent()
            }
            null -> {}
        }
    }

    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.online_search_title), color = c.text, fontWeight = FontWeight.Bold) },
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
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQuery,
                label = { Text(stringResource(R.string.online_search_field)) },
                leadingIcon = { TahakomIcon("search", c.textFaint, size = 20.dp) },
                singleLine = true,
                shape = RoundedCornerShape(tokens.shape.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.line,
                    focusedTextColor = c.text, unfocusedTextColor = c.text,
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                    focusedLabelColor = c.accent, unfocusedLabelColor = c.textFaint, cursorColor = c.accent,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(
                stringResource(R.string.online_search_hint), color = c.textFaint, fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
            )
            Spacer(Modifier.size(10.dp))

            // مرشّح نوع الجهاز: الكل/تلفاز/استقبال/صوت — لحصر النتائج حسب الهدف.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(stringResource(R.string.filter_all), catFilter == null) { catFilter = null }
                FilterChip(stringResource(R.string.cat_tv_short), catFilter == "TV") { catFilter = "TV" }
                FilterChip(stringResource(R.string.cat_cable_short), catFilter == "Cable") { catFilter = "Cable" }
                FilterChip(stringResource(R.string.cat_audio_short), catFilter == "Audio") { catFilter = "Audio" }
            }
            Spacer(Modifier.size(8.dp))

            val shown = state.results.filter { catFilter == null || it.category == catFilter }
            if (state.searched && shown.isEmpty()) {
                Text(
                    stringResource(R.string.online_no_results, state.query), color = c.textDim, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 24.dp, start = 4.dp),
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shown, key = { it.path }) { entry ->
                        OnlineRow(
                            entry = entry,
                            fetching = state.fetchingPath == entry.path,
                            onClick = { viewModel.fetch(entry) },
                        )
                    }
                    item(key = "tail") { Spacer(Modifier.size(8.dp)) }
                }
            }
        }
    }
}

/** شريحة مرشّح (مفعّلة = لون الإبراز). */
@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = tokens.colors
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) c.accent else c.surface)
            .border(1.dp, if (selected) c.accent else c.line, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label, color = if (selected) c.accentText else c.textDim,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OnlineRow(entry: OnlineEntry, fetching: Boolean, onClick: () -> Unit) {
    val c = tokens.colors
    val enabled = entry.supported && !fetching
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shape.lg)).background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(tokens.shape.lg))
            .clickable(enabled = enabled, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(tokens.shape.sm))
                .background(if (entry.supported) c.irSoft else c.bg2),
            contentAlignment = Alignment.Center,
        ) { TahakomIcon(if (entry.supported) "ir" else "close", if (entry.supported) c.ir else c.textFaint, size = 22.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                "${entry.brand} (${entry.tag})", color = if (entry.supported) c.text else c.textFaint,
                fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (entry.supported)
                    stringResource(R.string.online_codeset_meta, entry.type, entry.protocol, entry.functions)
                else stringResource(R.string.online_unsupported_tag),
                color = c.textFaint, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            fetching -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = c.accent)
            entry.supported -> Box(
                Modifier.clip(RoundedCornerShape(50)).background(c.accentSoft).padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) { TahakomIcon("plus", c.accent, size = 18.dp) }
        }
    }
}
