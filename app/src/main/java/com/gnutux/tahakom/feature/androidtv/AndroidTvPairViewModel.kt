package com.gnutux.tahakom.feature.androidtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gnutux.tahakom.core.transport.impl.androidtv.AndroidTvPairing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PairStage { CONNECTING, ENTER_CODE, SUBMITTING, ERROR, DONE }

data class AtvPairUiState(val stage: PairStage = PairStage.CONNECTING)

/**
 * يدير جلسة إقران Android TV (تجريبي): يبدأ الاتصال فيعرض التلفاز رمزاً، ثم يرسل الرمز.
 */
@HiltViewModel
class AndroidTvPairViewModel @Inject constructor() : ViewModel() {

    private val pairing = AndroidTvPairing()
    private val _uiState = MutableStateFlow(AtvPairUiState())
    val uiState: StateFlow<AtvPairUiState> = _uiState.asStateFlow()
    private var started = false

    fun start(host: String) {
        if (started) return
        started = true
        _uiState.update { it.copy(stage = PairStage.CONNECTING) }
        viewModelScope.launch {
            val ok = pairing.start(host)
            _uiState.update { it.copy(stage = if (ok) PairStage.ENTER_CODE else PairStage.ERROR) }
        }
    }

    fun submit(code: String) {
        if (code.isBlank()) return
        _uiState.update { it.copy(stage = PairStage.SUBMITTING) }
        viewModelScope.launch {
            val ok = pairing.submitCode(code)
            _uiState.update { it.copy(stage = if (ok) PairStage.DONE else PairStage.ERROR) }
        }
    }

    override fun onCleared() {
        pairing.cancel()
    }
}
