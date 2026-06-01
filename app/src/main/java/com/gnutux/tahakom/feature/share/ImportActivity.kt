package com.gnutux.tahakom.feature.share

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
import com.gnutux.tahakom.R
import com.gnutux.tahakom.core.share.PackScope
import com.gnutux.tahakom.core.share.RemotePack
import com.gnutux.tahakom.core.share.RemotePackSharing
import com.gnutux.tahakom.ui.theme.TahakomTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * يستقبل ملف/رابط `.tahakom` المشترَك ويعرض معاينة الاستيراد.
 *
 * عند نقر المستخدم على ملف مشترَك من شخص لديه نفس التطبيق، يفتح هنا مباشرة.
 * منطق الحفظ الفعلي في Room يُربط في م2 (عند توفّر مستودع الريموتات).
 */
@AndroidEntryPoint
class ImportActivity : ComponentActivity() {
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
                        onConfirm = {
                            // TODO(م2): حفظ الحزمة في مستودع الريموتات (Room) ثم فتح الجهاز.
                            finish()
                        },
                        onCancel = { finish() },
                    )
                }
            }
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
