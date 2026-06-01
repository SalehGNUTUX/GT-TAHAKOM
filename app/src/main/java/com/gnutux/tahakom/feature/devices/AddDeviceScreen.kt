package com.gnutux.tahakom.feature.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.catalog.BrandCatalog
import com.gnutux.tahakom.core.model.Device

/**
 * شاشة "إضافة بالاسم/الطراز": يختار المستخدم علامة من الكتالوج،
 * ويُدخل عنوان IP للأجهزة الشبكية، فيُنشأ [Device] ويُفتح ريموته.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    onBack: () -> Unit,
    onDeviceReady: (Device) -> Unit,
) {
    var selected by remember { mutableStateOf<BrandCatalog.Brand?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_device_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        val current = selected
        if (current == null) {
            LazyColumn(Modifier.padding(padding).padding(16.dp)) {
                items(BrandCatalog.brands, key = { it.name }) { brand ->
                    ElevatedCard(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selected = brand },
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(brand.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                brand.transport.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        } else {
            BrandDetail(
                brand = current,
                modifier = Modifier.padding(padding).padding(16.dp),
                onBack = { selected = null },
                onDeviceReady = onDeviceReady,
            )
        }
    }
}

@Composable
private fun BrandDetail(
    brand: BrandCatalog.Brand,
    modifier: Modifier,
    onBack: () -> Unit,
    onDeviceReady: (Device) -> Unit,
) {
    var address by remember { mutableStateOf("") }
    Column(modifier) {
        Text(brand.name, style = MaterialTheme.typography.headlineSmall)
        if (brand.needsAddress) {
            Text(
                stringResource(R.string.add_device_ip_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.add_device_ip_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                stringResource(R.string.add_device_ir_note),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        Button(
            onClick = {
                onDeviceReady(
                    Device(
                        id = "${brand.name}-${address.ifBlank { "ir" }}",
                        name = brand.name,
                        type = brand.type,
                        transport = brand.transport,
                        address = address.ifBlank { null },
                        metadata = mapOf("brand" to brand.name),
                    ),
                )
            },
            enabled = !brand.needsAddress || address.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.add_device_open_remote))
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}
