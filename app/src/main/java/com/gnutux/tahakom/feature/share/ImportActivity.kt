package com.gnutux.tahakom.feature.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.gnutux.tahakom.MainActivity
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.irdb.IrDatabase
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.model.DeviceType
import com.gnutux.tahakom.core.share.PackScope
import com.gnutux.tahakom.core.share.RemotePack
import com.gnutux.tahakom.core.share.RemotePackSharing
import com.gnutux.tahakom.core.store.SavedDevicesRepository
import com.gnutux.tahakom.core.transport.TransportType
import com.gnutux.tahakom.ui.theme.TahakomTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * يستقبل ملف/رابط `.tahakom` المشترَك ويعرض معاينة الاستيراد. عند التأكيد يُحفظ الجهاز
 * في «أجهزتي» (مطابقاً قاعدة IR إن أمكن) ويُفتح التطبيق.
 */
@AndroidEntryPoint
class ImportActivity : ComponentActivity() {

    @Inject lateinit var irDb: IrDatabase
    @Inject lateinit var saved: SavedDevicesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pack = RemotePackSharing.readFromIntent(this, intent)

        setContent {
            TahakomTheme {
                if (pack == null) {
                    ImportError(onClose = { finish() })
                } else {
                    ImportPreview(
                        pack = pack,
                        onConfirm = { importAndOpen(pack) },
                        onCancel = { finish() },
                    )
                }
            }
        }
    }

    /** يبني جهازاً من الحزمة (مطابقاً القاعدة إن أمكن)، يحفظه، ثم يفتح التطبيق. */
    private fun importAndOpen(pack: RemotePack) {
        lifecycleScope.launch {
            val match = runCatching {
                irDb.index().firstOrNull {
                    it.brand.equals(pack.brand, ignoreCase = true) ||
                        (pack.model != null && it.brand.equals(pack.model, ignoreCase = true))
                }
            }.getOrNull()
            val device = if (match != null) {
                Device(
                    id = "ir-${match.category}-${match.brand}", name = match.brand,
                    type = DeviceType.TV, transport = TransportType.IR,
                    metadata = mapOf("brand" to match.brand, "category" to match.category, "irFile" to match.file),
                )
            } else {
                val name = pack.model ?: pack.brand
                Device(
                    id = "import-$name", name = name, type = DeviceType.TV, transport = TransportType.IR,
                    metadata = mapOf("brand" to pack.brand, "category" to "TV"),
                )
            }
            runCatching { saved.add(device) }
            startActivity(
                Intent(this@ImportActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
            finish()
        }
    }
}

@Composable
private fun ImportPreview(pack: RemotePack, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.import_title),
                style = MaterialTheme.typography.titleLarge,
            )
            val scopeLabel = when (pack.scope) {
                PackScope.BRAND -> stringResource(R.string.import_scope_brand, pack.brand)
                PackScope.MODEL -> stringResource(
                    R.string.import_scope_model, pack.brand, pack.model ?: "",
                )
            }
            Text(text = scopeLabel, modifier = Modifier.padding(top = 8.dp))
            Text(
                text = stringResource(R.string.import_remotes_count, pack.remotesJson.size),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            pack.description?.let {
                Text(text = it, modifier = Modifier.padding(top = 8.dp))
            }
            Button(onClick = onConfirm, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.import_confirm))
            }
            Button(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}

@Composable
private fun ImportError(onClose: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.import_error),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onClose, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}
