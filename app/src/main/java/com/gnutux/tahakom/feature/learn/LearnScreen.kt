package com.gnutux.tahakom.feature.learn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.model.Device

/**
 * شاشة "تعلّم ريموت يدوياً": المستخدم يُدخل اسم الجهاز وكود Pronto لكل زر،
 * فيُحفظ كجهاز IR مخصّص (للأجهزة غير الموثّقة مثل Unionaire). بلا عتاد إضافي.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    onBack: () -> Unit,
    onSaved: (Device) -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.learn_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.learn_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                OutlinedTextField(
                    value = state.brand,
                    onValueChange = viewModel::setBrand,
                    label = { Text(stringResource(R.string.learn_device_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                Text(
                    stringResource(R.string.learn_codes_label),
                    fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            items(state.entries, key = { it.button.name }) { entry ->
                OutlinedTextField(
                    value = entry.code,
                    onValueChange = { viewModel.setCode(entry.button, it) },
                    label = { Text(stringResource(entry.labelRes)) },
                    placeholder = { Text("0000 006D 0022 …") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }

            item {
                state.error?.let {
                    val msg = when (it) {
                        "empty_brand" -> stringResource(R.string.learn_err_name)
                        "no_valid_code" -> stringResource(R.string.learn_err_codes)
                        else -> it
                    }
                    Text(msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                }
                Button(
                    onClick = { viewModel.save()?.let(onSaved) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                ) { Text(stringResource(R.string.learn_save)) }
            }
        }
    }
}
