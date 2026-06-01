package com.gnutux.tahakom.feature.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gnutux.tahakom.core.model.Command
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.transport.TransportRegistry
import com.gnutux.tahakom.core.transport.TransportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** آخر نتيجة إرسال (لعرض تغذية راجعة للمستخدم). */
data class RemoteUiState(
    val lastSentLabel: String? = null,
    val lastError: String? = null,
)

/**
 * يدير إرسال أوامر الريموت للجهاز المختار عبر [TransportRegistry].
 * يربط الواجهة بطبقة النقل: الواجهة ترسل [Command] مجرّداً والـ Transport يترجمه.
 */
@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val registry: TransportRegistry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    fun send(device: Device, command: Command, label: String) {
        val transport = registry.forDevice(device) ?: run {
            _uiState.update { it.copy(lastError = "transport_unavailable") }
            return
        }
        viewModelScope.launch {
            when (val result = transport.send(device, command)) {
                is TransportResult.Success ->
                    _uiState.update { RemoteUiState(lastSentLabel = label) }
                is TransportResult.Failure ->
                    _uiState.update { it.copy(lastError = result.error.name) }
            }
        }
    }
}
