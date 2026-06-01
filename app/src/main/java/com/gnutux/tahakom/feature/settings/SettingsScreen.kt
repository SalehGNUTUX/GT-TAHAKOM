package com.gnutux.tahakom.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.settings.AppLanguage
import com.gnutux.tahakom.core.settings.LocaleManager

/**
 * شاشة الإعدادات — تتضمّن مبدّل اللغة (عربي/إنجليزي/النظام).
 * التبديل فوري عبر [LocaleManager]؛ يعيد النظام إنشاء الواجهة باللغة الجديدة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
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
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            var selected by remember { mutableStateOf(LocaleManager.current()) }
            LanguageOption(AppLanguage.SYSTEM, R.string.language_system, selected) {
                selected = it; LocaleManager.apply(it)
            }
            LanguageOption(AppLanguage.ARABIC, R.string.language_arabic, selected) {
                selected = it; LocaleManager.apply(it)
            }
            LanguageOption(AppLanguage.ENGLISH, R.string.language_english, selected) {
                selected = it; LocaleManager.apply(it)
            }
        }
    }
}

@Composable
private fun LanguageOption(
    language: AppLanguage,
    labelRes: Int,
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected == language, onClick = { onSelect(language) })
            .padding(vertical = 12.dp),
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected == language, onClick = { onSelect(language) })
            Text(
                text = stringResource(labelRes),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
