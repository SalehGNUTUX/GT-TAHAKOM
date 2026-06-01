package com.gnutux.tahakom

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.gnutux.tahakom.core.discovery.toDevice
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.share.PackScope
import com.gnutux.tahakom.core.share.RemotePack
import com.gnutux.tahakom.core.share.RemotePackSharing
import com.gnutux.tahakom.feature.devices.AddDeviceScreen
import com.gnutux.tahakom.feature.devices.DevicesScreen
import com.gnutux.tahakom.feature.devices.DevicesViewModel
import com.gnutux.tahakom.feature.irsetup.IrSetupScreen
import com.gnutux.tahakom.feature.onboarding.OnboardingScreen
import com.gnutux.tahakom.feature.remote.RemoteScreen
import com.gnutux.tahakom.feature.settings.SettingsScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.ui.theme.TahakomTheme
import dagger.hilt.android.AndroidEntryPoint

/** الوجهات المتاحة في التنقّل المبسّط. */
private sealed interface Screen {
    data object Devices : Screen
    data object Settings : Screen
    data object AddDevice : Screen
    data class IrSetup(val irFile: String) : Screen
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
                val context = LocalContext.current
                // ViewModel مشترك على مستوى الشاشة الرئيسية للحفظ/المشاركة.
                val devicesVm: DevicesViewModel = hiltViewModel()
                val onboardingDone by devicesVm.onboardingDone.collectAsStateWithLifecycle()
                var screen by remember { mutableStateOf<Screen>(Screen.Devices) }

                // شاشة الترحيب عند أول تشغيل (تنتظر تحميل الحالة لتفادي وميض).
                if (onboardingDone == false) {
                    OnboardingScreen(onDone = { devicesVm.completeOnboarding() })
                    return@TahakomTheme
                }
                if (onboardingDone == null) return@TahakomTheme // لا تومض قبل المعرفة

                // يعتمد جهازاً: يحفظه في القائمة ثم يفتح ريموته.
                fun adopt(device: Device) {
                    devicesVm.save(device)
                    screen = Screen.Remote(device)
                }

                fun shareDevice(device: Device) {
                    val pack = RemotePack(
                        scope = PackScope.MODEL,
                        brand = device.metadata["brand"] ?: device.name,
                        model = device.name,
                        description = device.name,
                        author = "GT-TAHAKOM",
                    )
                    val intent = RemotePackSharing.exportToShareIntent(context, pack)
                    context.startActivity(Intent.createChooser(intent, device.name))
                }

                when (val s = screen) {
                    Screen.Devices -> DevicesScreen(
                        onOpenSettings = { screen = Screen.Settings },
                        onAddManual = { screen = Screen.AddDevice },
                        onOpenDevice = { screen = Screen.Remote(it) },
                        onAdoptDiscovered = { adopt(it.toDevice()) },
                        onShareDevice = { shareDevice(it) },
                        viewModel = devicesVm,
                    )
                    Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Devices })
                    Screen.AddDevice -> AddDeviceScreen(
                        onBack = { screen = Screen.Devices },
                        onPickIrDevice = { screen = Screen.IrSetup(it.file) },
                    )
                    is Screen.IrSetup -> IrSetupScreen(
                        irFile = s.irFile,
                        onBack = { screen = Screen.AddDevice },
                        onDeviceReady = { adopt(it) },
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
