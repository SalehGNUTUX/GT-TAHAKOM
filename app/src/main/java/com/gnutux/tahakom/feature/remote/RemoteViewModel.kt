package com.gnutux.tahakom.feature.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gnutux.tahakom.core.irdb.IrDatabase
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Command
import com.gnutux.tahakom.core.model.Device
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
            // الأجهزة الشبكية: كل الأزرار متاحة، لا تحميل.
            _uiState.update { RemoteUiState(supported = ButtonId.entries.toSet(), ready = true) }
            return
        }

        val file = device.metadata["irFile"]
        if (file == null) {
            _uiState.update { it.copy(ready = true, lastError = "ir_file_missing") }
            return
        }
        viewModelScope.launch {
            val entry = irDb.index().firstOrNull { it.file == file }
            if (entry == null) {
                _uiState.update { it.copy(ready = true, lastError = "ir_device_not_found") }
                return@launch
            }
            val irDevice = irDb.loadDevice(entry)
            // أول كود لكل معرّف دلالي (قد يتكرّر المعرّف في بعض القواعد).
            irCodes = irDevice.buttons
                .mapNotNull { b -> runCatching { ButtonId.valueOf(b.id) }.getOrNull()?.let { it to b.code } }
                .toMap()
            _uiState.update {
                RemoteUiState(supported = irCodes.keys, ready = true)
            }
        }
    }

    fun send(device: Device, button: ButtonId, label: String) {
        val transport = registry.forDevice(device) ?: run {
            _uiState.update { it.copy(lastError = "transport_unavailable") }
            return
        }
        // ترجمة الأمر حسب الوسيلة.
        val command: Command = if (device.transport == TransportType.IR) {
            val code = irCodes[button] ?: run {
                _uiState.update { it.copy(lastError = "UNSUPPORTED_COMMAND") }
                return
            }
            Command.Raw(code)
        } else {
            Command.Key(button)
        }
        viewModelScope.launch {
            when (val result = transport.send(device, command)) {
                is TransportResult.Success ->
                    _uiState.update { it.copy(lastSentLabel = label, lastError = null) }
                is TransportResult.Failure ->
                    _uiState.update { it.copy(lastError = result.error.name) }
            }
        }
    }
}
