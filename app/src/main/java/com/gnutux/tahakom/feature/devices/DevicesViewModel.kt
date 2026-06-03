package com.gnutux.tahakom.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gnutux.tahakom.core.discovery.DiscoveredDevice
import com.gnutux.tahakom.core.discovery.DiscoveryManager
import com.gnutux.tahakom.core.discovery.key
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.store.SavedDevicesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoveryUiState(
    val isScanning: Boolean = false,
    val discovered: List<DiscoveredDevice> = emptyList(),
)

/**
 * يدير الشاشة الرئيسية: الأجهزة **المحفوظة** (التي اعتمدها المستخدم) + المسح الحيّ.
 * يجمع بين [SavedDevicesRepository] و[DiscoveryManager].
 */
@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val saved: SavedDevicesRepository,
    private val discoveryManager: DiscoveryManager,
) : ViewModel() {

    /** الأجهزة المحفوظة (مصدرها DataStore، تتحدّث تلقائياً). */
    val savedDevices: StateFlow<List<Device>> =
        saved.devices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** هل أنهى المستخدم الترحيب؟ null = لم يُحمَّل بعد. */
    val onboardingDone: StateFlow<Boolean?> =
        saved.onboardingDone.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun completeOnboarding() = viewModelScope.launch { saved.setOnboardingDone() }

    fun resetOnboarding() = viewModelScope.launch { saved.resetOnboarding() }

    /** وضع السمة (system/light/dark) — يُطبَّق في MainActivity. */
    val themeMode: StateFlow<String> =
        saved.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    fun setThemeMode(mode: String) = viewModelScope.launch { saved.setThemeMode(mode) }

    private val _discovery = MutableStateFlow(DiscoveryUiState())
    val discovery: StateFlow<DiscoveryUiState> = _discovery.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        scanJob?.cancel()
        _discovery.update { DiscoveryUiState(isScanning = true, discovered = emptyList()) }
        val seen = LinkedHashMap<String, DiscoveredDevice>()
        scanJob = viewModelScope.launch {
            discoveryManager.discoverAll().collect { device ->
                seen.putIfAbsent(device.key, device)
                _discovery.update { it.copy(discovered = seen.values.toList()) }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _discovery.update { it.copy(isScanning = false) }
    }

    fun save(device: Device) = viewModelScope.launch { saved.add(device) }
    fun remove(deviceId: String) = viewModelScope.launch { saved.remove(deviceId) }
    fun reorder(ordered: List<Device>) = viewModelScope.launch { saved.reorder(ordered) }

    /** ينقل جهازاً من موضع إلى آخر في قائمة "أجهزتي". */
    fun move(from: Int, to: Int) = viewModelScope.launch {
        val list = savedDevices.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return@launch
        list.add(to, list.removeAt(from))
        saved.reorder(list)
    }

    override fun onCleared() {
        scanJob?.cancel()
    }
}
