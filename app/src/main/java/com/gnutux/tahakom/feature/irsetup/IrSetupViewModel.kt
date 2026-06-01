package com.gnutux.tahakom.feature.irsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gnutux.tahakom.core.irdb.IrCommandResolver
import com.gnutux.tahakom.core.irdb.IrDatabase
import com.gnutux.tahakom.core.irdb.IrDevice
import com.gnutux.tahakom.core.irdb.IrDeviceEntry
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.transport.TransportRegistry
import com.gnutux.tahakom.core.transport.TransportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** هل الجهاز المُراد ضبطه مشغّل أم مطفأ؟ يحدّد إشارة الاختبار. */
enum class PowerState { OFF, ON }

data class IrSetupUiState(
    val irAvailable: Boolean = true,
    val categories: List<String> = emptyList(),
    val devices: List<IrDeviceEntry> = emptyList(),
    val category: String? = null,
    /** مؤشّر الجهاز الحالي الجاري اختباره ضمن [devices]. */
    val testIndex: Int = 0,
    val testingBrand: String? = null,
    /** الإشارة التي تُرسَل للاختبار حسب حالة الطاقة. */
    val testButtonLabel: String? = null,
)

/**
 * اكتشاف جهاز IR شبه آلي (إذ يستحيل الاكتشاف التلقائي — الـ IR أحادي الاتجاه).
 *
 * المنطق المعتمد عملياً: نسأل المستخدم **هل الجهاز مشغّل؟**
 * - مطفأ  → نرسل [ButtonId.POWER] (يراقب: هل اشتغل؟).
 * - مشغّل → نرسل [ButtonId.VOL_UP] أو [ButtonId.CH_UP] (تغيّر ملحوظ بلا إطفاء).
 * يتنقّل المستخدم بين العلامات حتى يستجيب جهازه، فتُحفظ العلامة الصحيحة.
 */
@HiltViewModel
class IrSetupViewModel @Inject constructor(
    private val db: IrDatabase,
    private val registry: TransportRegistry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IrSetupUiState())
    val uiState: StateFlow<IrSetupUiState> = _uiState.asStateFlow()

    private var loadedDevice: IrDevice? = null

    init {
        viewModelScope.launch {
            val ir = registry.forType(TransportType.IR)?.isAvailable() ?: false
            _uiState.update { it.copy(irAvailable = ir, categories = db.categories()) }
        }
    }

    fun selectCategory(category: String) {
        viewModelScope.launch {
            val devices = db.devicesIn(category)
            _uiState.update {
                it.copy(category = category, devices = devices, testIndex = 0)
            }
            prepareTest(0)
        }
    }

    fun nextBrand() {
        val s = _uiState.value
        if (s.testIndex + 1 < s.devices.size) prepareTest(s.testIndex + 1)
    }

    fun previousBrand() {
        val s = _uiState.value
        if (s.testIndex > 0) prepareTest(s.testIndex - 1)
    }

    private fun prepareTest(index: Int) {
        val entry = _uiState.value.devices.getOrNull(index) ?: return
        viewModelScope.launch {
            loadedDevice = db.loadDevice(entry)
            _uiState.update {
                it.copy(testIndex = index, testingBrand = entry.brand)
            }
        }
    }

    /**
     * يرسل إشارة الاختبار حسب حالة الطاقة:
     * مطفأ → POWER، مشغّل → VOL_UP (أو CH_UP إن لم يدعم الصوت).
     */
    fun sendTest(power: PowerState) {
        val device = loadedDevice ?: return
        val transport = registry.forType(TransportType.IR) ?: return

        val button = when (power) {
            PowerState.OFF -> ButtonId.POWER
            PowerState.ON -> if (device.buttons.any { it.id == ButtonId.VOL_UP.name }) {
                ButtonId.VOL_UP
            } else {
                ButtonId.CH_UP
            }
        }
        val command = IrCommandResolver.resolve(device, button) ?: return
        viewModelScope.launch {
            transport.send(buildDevice(device), command)
            _uiState.update { it.copy(testButtonLabel = button.name) }
        }
    }

    /** يثبّت العلامة الحالية كجهاز قابل للاستخدام (بعد أن استجاب). */
    fun confirmDevice(): Device? = loadedDevice?.let { buildDevice(it) }

    private fun buildDevice(d: IrDevice): Device = Device(
        id = "ir-${d.category}-${d.brand}",
        name = d.brand,
        type = com.gnutux.tahakom.core.model.DeviceType.TV,
        transport = TransportType.IR,
        address = null,
        metadata = mapOf("brand" to d.brand, "category" to d.category, "irFile" to d.model),
    )
}
