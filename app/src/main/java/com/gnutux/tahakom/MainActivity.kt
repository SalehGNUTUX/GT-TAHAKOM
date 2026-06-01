package com.gnutux.tahakom

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gnutux.tahakom.feature.devices.DevicesScreen
import com.gnutux.tahakom.feature.settings.SettingsScreen
import com.gnutux.tahakom.ui.theme.TahakomTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * النشاط الوحيد — يستضيف واجهة Compose.
 * يرث [AppCompatActivity] كي تعمل آلية تبديل اللغة (per-app locale) على كل الإصدارات.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TahakomTheme {
                // تنقّل مبسّط ريثما يُضاف Navigation الكامل في م2.
                var showSettings by remember { mutableStateOf(false) }
                if (showSettings) {
                    SettingsScreen(onBack = { showSettings = false })
                } else {
                    DevicesScreen(onOpenSettings = { showSettings = true })
                }
            }
        }
    }
}
