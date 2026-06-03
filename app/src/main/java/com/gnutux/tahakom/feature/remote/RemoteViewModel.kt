package com.gnutux.tahakom.feature.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gnutux.tahakom.core.irdb.IrDatabase
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Command
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.transport.TransportError
import com.gnutux.tahakom.core.transport.TransportRegistry
import com.gnutux.tahakom.core.transport.TransportResult
import com.gnutux.tahakom.core.transport.TransportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** حالة شاشة الريموت. */
data class RemoteUiState(
    val lastSentLabel: String? = null,
    val lastError: String? = null,
    /** المعرّفات الدلالية المدعومة فعلياً لهذا الجهاز (لإظهار الأزرار المتاحة فقط). */
    val supported: Set<ButtonId> = ButtonId.entries.toSet(),
    val ready: Boolean = false,
    /** Android TV (تجريبي): يحتاج إقراناً برمز 6 خانات قبل التحكّم. */
    val needsPairing: Boolean = false,
)

/**
 * يدير إرسال أوامر الريموت. للأجهزة الشبكية يمرّر [Command.Key] مباشرة (الـ Transport
 * يترجمه). **لأجهزة IR** يحمّل الجهاز من [IrDatabase] ويترجم الزر الدلالي إلى كود
 * Pronto (Command.Raw) قبل الإرسال — هذا ما كان مفقوداً وسبّب UNSUPPORTED_COMMAND.
 */
@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val registry: TransportRegistry,
    private val irDb: IrDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    /** خريطة الزر الدلالي → كود Pronto، تُملأ لأجهزة IR عند الربط. */
    private var irCodes: Map<ButtonId, String> = emptyMap()
    private var boundDeviceId: String? = null

    /** يُستدعى عند فتح شاشة الريموت لتحميل أكواد IR إن لزم. */
    fun bind(device: Device) {
        if (boundDeviceId == device.id) return
        boundDeviceId = device.id

        if (device.transport != TransportType.IR) {
            // الأجهزة الشبكية: الأزرار المدعومة حسب البروتوكول (مدفوع بالبيانات، عام).
            _uiState.update {
                RemoteUiState(supported = supportedForTransport(device.transport), ready = true)
            }
            // Android TV (تجريبي): افحص إن كان يحتاج إقراناً ليُظهِر التطبيق دعوة الإقران.
            if (device.transport == TransportType.ANDROID_TV) {
                viewModelScope.launch {
                    val r = registry.forDevice(device)?.connect(device)
                    if (r is TransportResult.Failure && r.error == TransportError.PAIRING_REQUIRED) {
                        _uiState.update { it.copy(needsPairing = true) }
                    }
                }
            }
            return
        }

        irCodes = emptyMap()
        viewModelScope.launch { ensureLoaded(device) }
    }

    fun send(device: Device, button: ButtonId, label: String) {
        val transport = registry.forDevice(device) ?: run {
            _uiState.update { it.copy(lastError = "transport_unavailable") }
            return
        }
        viewModelScope.launch {
            // لأجهزة IR: تأكّد أن الأكواد حُمِّلت قبل الإرسال (يحلّ مشكلة "النقر مرتين"؛
            // أول نقرة كانت تسبق اكتمال التحميل غير المتزامن فتفشل صامتةً).
            if (device.transport == TransportType.IR && irCodes.isEmpty()) {
                ensureLoaded(device)
            }
            val command: Command = if (device.transport == TransportType.IR) {
                val code = irCodes[button] ?: run {
                    _uiState.update { it.copy(lastError = "UNSUPPORTED_COMMAND") }
                    return@launch
                }
                Command.Raw(code)
            } else {
                Command.Key(button)
            }
            when (val result = transport.send(device, command)) {
                is TransportResult.Success ->
                    _uiState.update { it.copy(lastSentLabel = label, lastError = null) }
                is TransportResult.Failure ->
                    _uiState.update { it.copy(lastError = result.error.name) }
            }
        }
    }

    /** يضمن تحميل أكواد IR (يُستدعى من bind ومن send عند الحاجة). */
    private suspend fun ensureLoaded(device: Device) {
        if (irCodes.isNotEmpty()) return
        val file = device.metadata["irFile"] ?: return
        val entry = irDb.index().firstOrNull { it.file == file } ?: return
        val irDevice = irDb.loadDevice(entry)
        irCodes = irDevice.buttons
            .mapNotNull { b -> runCatching { ButtonId.valueOf(b.id) }.getOrNull()?.let { it to b.code } }
            .toMap()
        _uiState.update { it.copy(supported = irCodes.keys, ready = true) }
    }

    /** الأزرار المدعومة لكل بروتوكول شبكي (يطابق ما يترجمه الـ Transport فعلياً). */
    private fun supportedForTransport(transport: TransportType): Set<ButtonId> = when (transport) {
        TransportType.LG_WEBOS -> setOf(
            ButtonId.POWER, ButtonId.VOL_UP, ButtonId.VOL_DOWN, ButtonId.MUTE,
            ButtonId.CH_UP, ButtonId.CH_DOWN, ButtonId.HOME, ButtonId.APPS,
            ButtonId.INFO, ButtonId.PLAY, ButtonId.PAUSE, ButtonId.STOP,
            ButtonId.FFWD, ButtonId.RWD, ButtonId.LIST, ButtonId.SETTINGS,
            // التنقّل عبر pointer input socket (أصبح مدعوماً).
            ButtonId.NAV_UP, ButtonId.NAV_DOWN, ButtonId.NAV_LEFT, ButtonId.NAV_RIGHT,
            ButtonId.NAV_OK, ButtonId.BACK, ButtonId.MENU, ButtonId.EXIT,
        )
        TransportType.ROKU -> setOf(
            ButtonId.POWER, ButtonId.NAV_UP, ButtonId.NAV_DOWN, ButtonId.NAV_LEFT,
            ButtonId.NAV_RIGHT, ButtonId.NAV_OK, ButtonId.BACK, ButtonId.HOME,
            ButtonId.INFO, ButtonId.VOL_UP, ButtonId.VOL_DOWN, ButtonId.MUTE,
            ButtonId.PLAY, ButtonId.FFWD, ButtonId.RWD,
            ButtonId.DIGIT_0, ButtonId.DIGIT_1, ButtonId.DIGIT_2, ButtonId.DIGIT_3, ButtonId.DIGIT_4,
            ButtonId.DIGIT_5, ButtonId.DIGIT_6, ButtonId.DIGIT_7, ButtonId.DIGIT_8, ButtonId.DIGIT_9,
        )
        TransportType.SAMSUNG_TIZEN -> setOf(
            ButtonId.POWER, ButtonId.NAV_UP, ButtonId.NAV_DOWN, ButtonId.NAV_LEFT,
            ButtonId.NAV_RIGHT, ButtonId.NAV_OK, ButtonId.BACK, ButtonId.HOME, ButtonId.MENU,
            ButtonId.EXIT, ButtonId.INFO, ButtonId.GUIDE, ButtonId.SOURCE, ButtonId.SMART,
            ButtonId.TOOLS, ButtonId.LIST, ButtonId.VOL_UP, ButtonId.VOL_DOWN, ButtonId.MUTE,
            ButtonId.CH_UP, ButtonId.CH_DOWN, ButtonId.PLAY, ButtonId.PAUSE, ButtonId.STOP,
            ButtonId.FFWD, ButtonId.RWD,
            ButtonId.DIGIT_0, ButtonId.DIGIT_1, ButtonId.DIGIT_2, ButtonId.DIGIT_3, ButtonId.DIGIT_4,
            ButtonId.DIGIT_5, ButtonId.DIGIT_6, ButtonId.DIGIT_7, ButtonId.DIGIT_8, ButtonId.DIGIT_9,
            ButtonId.RED, ButtonId.GREEN, ButtonId.YELLOW, ButtonId.BLUE,
        )
        TransportType.ANDROID_TV -> setOf(
            ButtonId.POWER, ButtonId.HOME, ButtonId.BACK, ButtonId.MENU,
            ButtonId.NAV_UP, ButtonId.NAV_DOWN, ButtonId.NAV_LEFT, ButtonId.NAV_RIGHT, ButtonId.NAV_OK,
            ButtonId.VOL_UP, ButtonId.VOL_DOWN, ButtonId.MUTE, ButtonId.CH_UP, ButtonId.CH_DOWN,
            ButtonId.PLAY, ButtonId.PAUSE, ButtonId.STOP, ButtonId.FFWD, ButtonId.RWD,
            ButtonId.INFO, ButtonId.GUIDE, ButtonId.SOURCE,
            ButtonId.DIGIT_0, ButtonId.DIGIT_1, ButtonId.DIGIT_2, ButtonId.DIGIT_3, ButtonId.DIGIT_4,
            ButtonId.DIGIT_5, ButtonId.DIGIT_6, ButtonId.DIGIT_7, ButtonId.DIGIT_8, ButtonId.DIGIT_9,
        )
        // بروتوكولات أخرى مستقبلاً: مجموعتها الخاصة. مؤقتاً كل الأزرار.
        else -> ButtonId.entries.toSet()
    }
}
