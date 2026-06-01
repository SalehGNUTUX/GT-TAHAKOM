package com.gnutux.tahakom.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gnutux.tahakom.core.discovery.DiscoveredDevice
import com.gnutux.tahakom.core.discovery.DiscoveryManager
import com.gnutux.tahakom.core.discovery.key
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** حالة شاشة الاكتشاف. */
data class DiscoveryUiState(
    val isScanning: Boolean = false,
    val devices: List<DiscoveredDevice> = emptyList(),
)

/**
 * يدير مسح الشبكة عبر [DiscoveryManager] ويجمّع النتائج (مع إزالة التكرار).
 * المسح أوفلاين بالكامل — لا إنترنت ولا قاعدة بيانات (انظر docs/DATABASE.md).
 */
@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val discoveryManager: DiscoveryManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    /** يبدأ مسحاً جديداً. يلغي أي مسح جارٍ ويمسح القائمة. */
    fun startScan() {
        scanJob?.cancel()
        _uiState.update { DiscoveryUiState(isScanning = true, devices = emptyList()) }
        val seen = LinkedHashMap<String, DiscoveredDevice>()
        scanJob = viewModelScope.launch {
            discoveryManager.discoverAll().collect { device ->
                seen.putIfAbsent(device.key, device)
                _uiState.update { it.copy(devices = seen.values.toList()) }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
    }

    override fun onCleared() {
        scanJob?.cancel()
    }
}
