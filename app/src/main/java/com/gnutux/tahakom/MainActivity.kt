package com.gnutux.tahakom

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gnutux.tahakom.core.discovery.toDevice
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.feature.devices.AddDeviceScreen
import com.gnutux.tahakom.feature.devices.DevicesScreen
import com.gnutux.tahakom.feature.remote.RemoteScreen
import com.gnutux.tahakom.feature.settings.SettingsScreen
import com.gnutux.tahakom.ui.theme.TahakomTheme
import dagger.hilt.android.AndroidEntryPoint

/** الوجهات المتاحة في التنقّل المبسّط. */
private sealed interface Screen {
    data object Devices : Screen
    data object Settings : Screen
    data object AddDevice : Screen
    data class Remote(val device: Device) : Screen
}

/**
 * النشاط الوحيد — يستضيف واجهة Compose.
 * يرث [AppCompatActivity] كي تعمل آلية تبديل اللغة (per-app locale).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TahakomTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Devices) }
                when (val s = screen) {
                    Screen.Devices -> DevicesScreen(
                        onOpenSettings = { screen = Screen.Settings },
                        onAddManual = { screen = Screen.AddDevice },
                        onDeviceClick = { screen = Screen.Remote(it.toDevice()) },
                    )
                    Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Devices })
                    Screen.AddDevice -> AddDeviceScreen(
                        onBack = { screen = Screen.Devices },
                        onDeviceReady = { screen = Screen.Remote(it) },
                    )
                    is Screen.Remote -> RemoteScreen(
                        device = s.device,
                        onBack = { screen = Screen.Devices },
                    )
                }
            }
        }
    }
}
