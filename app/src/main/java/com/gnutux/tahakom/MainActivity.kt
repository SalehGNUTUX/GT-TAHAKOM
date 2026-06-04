package com.gnutux.tahakom

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.gnutux.tahakom.core.discovery.toDevice
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.share.PackScope
import com.gnutux.tahakom.core.share.RemotePack
import com.gnutux.tahakom.core.share.RemotePackSharing
import com.gnutux.tahakom.feature.devices.AddDeviceScreen
import com.gnutux.tahakom.feature.devices.AddNetworkScreen
import com.gnutux.tahakom.feature.devices.DevicesScreen
import com.gnutux.tahakom.feature.devices.DevicesViewModel
import com.gnutux.tahakom.feature.devices.ScanScreen
import com.gnutux.tahakom.feature.androidtv.AndroidTvPairScreen
import com.gnutux.tahakom.feature.irsetup.IrSetupScreen
import com.gnutux.tahakom.feature.online.OnlineSearchScreen
import com.gnutux.tahakom.feature.learn.LearnScreen
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
    data object AddNetwork : Screen
    data object Learn : Screen
    data object OnlineSearch : Screen
    data object Scan : Screen
    data class IrSetup(val irFile: String) : Screen
    data class Remote(val device: Device) : Screen
    data class AndroidTvPair(val device: Device) : Screen
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
            // ViewModel مشترك يُنشأ قبل السمة كي تقرأ وضع السمة المحفوظ منه.
            val devicesVm: DevicesViewModel = hiltViewModel()
            val themeMode by devicesVm.themeMode.collectAsStateWithLifecycle()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val dark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }
            TahakomTheme(darkTheme = dark) {
                val context = LocalContext.current
                val onboardingDone by devicesVm.onboardingDone.collectAsStateWithLifecycle()
                // مسار محفوظ يصمد لإعادة إنشاء النشاط (عند تبديل اللغة) — يُبقي شاشة
                // الإعدادات كما هي بدل العودة للرئيسية. الشاشات الحاملة لبيانات (Remote)
                // لا تُحفظ فتعود للرئيسية (مقبول؛ اللغة تُغيَّر من الإعدادات فقط).
                var route by rememberSaveable { mutableStateOf("devices") }
                var screen by remember {
                    mutableStateOf<Screen>(
                        when (route) {
                            "settings" -> Screen.Settings
                            "add" -> Screen.AddDevice
                            "learn" -> Screen.Learn
                            else -> Screen.Devices
                        },
                    )
                }
                LaunchedEffect(screen) {
                    route = when (screen) {
                        Screen.Settings -> "settings"
                        Screen.AddDevice -> "add"
                        Screen.Learn -> "learn"
                        else -> "devices"
                    }
                }

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

                // زر رجوع الهاتف: من أي شاشة فرعية → يعود للرئيسية؛ من الرئيسية →
                // نقرتان خلال ثانيتين للخروج (مع رسالة تأكيد).
                var lastBackMs by remember { mutableLongStateOf(0L) }
                val exitHint = stringResource(R.string.exit_hint)
                BackHandler(enabled = true) {
                    if (screen != Screen.Devices) {
                        screen = when (val cur = screen) {
                            is Screen.IrSetup, Screen.Learn, Screen.AddNetwork, Screen.OnlineSearch -> Screen.AddDevice
                            is Screen.AndroidTvPair -> Screen.Remote(cur.device)
                            else -> Screen.Devices
                        }
                    } else {
                        val now = System.currentTimeMillis()
                        if (now - lastBackMs < 2000) {
                            finish()
                        } else {
                            lastBackMs = now
                            Toast.makeText(context, exitHint, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                when (val s = screen) {
                    Screen.Devices -> DevicesScreen(
                        onOpenSettings = { screen = Screen.Settings },
                        onAddManual = { screen = Screen.AddDevice },
                        onScan = { screen = Screen.Scan },
                        onOpenDevice = { screen = Screen.Remote(it) },
                        onShareDevice = { shareDevice(it) },
                        viewModel = devicesVm,
                    )
                    Screen.Scan -> ScanScreen(
                        onBack = { screen = Screen.Devices },
                        // الإضافة من المسح: تحفظ في «أجهزتي» وتفتح ريموت الجهاز مباشرةً (كبقية المسارات).
                        onAdopt = { adopt(it.toDevice()) },
                        viewModel = devicesVm,
                    )
                    Screen.Settings -> SettingsScreen(
                        onBack = { screen = Screen.Devices },
                        themeMode = themeMode,
                        onThemeMode = devicesVm::setThemeMode,
                        onReplayOnboarding = {
                            devicesVm.resetOnboarding()
                            screen = Screen.Devices // الترحيب يُعرَض تلقائياً عند صفر العَلَم
                        },
                        onDeviceReady = { adopt(it) },
                    )
                    Screen.AddDevice -> AddDeviceScreen(
                        onBack = { screen = Screen.Devices },
                        onPickIrDevice = { screen = Screen.IrSetup(it.file) },
                        onLearn = { screen = Screen.Learn },
                        onAddNetwork = { screen = Screen.AddNetwork },
                        onSearchOnline = { screen = Screen.OnlineSearch },
                        onDeviceReady = { adopt(it) },
                    )
                    Screen.OnlineSearch -> OnlineSearchScreen(
                        onBack = { screen = Screen.AddDevice },
                        // بعد جلب جهاز جديد وحفظه محلياً، افتحه في شاشة الضبط لاختباره فوراً.
                        onOpenSetup = { screen = Screen.IrSetup(it) },
                    )
                    Screen.AddNetwork -> AddNetworkScreen(
                        onBack = { screen = Screen.AddDevice },
                        onDeviceReady = { adopt(it) },
                    )
                    Screen.Learn -> LearnScreen(
                        onBack = { screen = Screen.AddDevice },
                        onSaved = { adopt(it) },
                    )
                    is Screen.IrSetup -> IrSetupScreen(
                        irFile = s.irFile,
                        onBack = { screen = Screen.AddDevice },
                        onDeviceReady = { adopt(it) },
                    )
                    is Screen.Remote -> RemoteScreen(
                        device = s.device,
                        onBack = { screen = Screen.Devices },
                        onPair = { screen = Screen.AndroidTvPair(it) },
                    )
                    is Screen.AndroidTvPair -> AndroidTvPairScreen(
                        host = s.device.address ?: "",
                        onBack = { screen = Screen.Remote(s.device) },
                        onPaired = { screen = Screen.Remote(s.device) },
                    )
                }
            }
        }
    }
}
