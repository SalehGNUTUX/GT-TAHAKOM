package com.gnutux.tahakom.feature.irsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gnutux.tahakom.core.irdb.IrCommandResolver
import com.gnutux.tahakom.core.irdb.IrDatabase
import com.gnutux.tahakom.core.irdb.IrDevice
import com.gnutux.tahakom.core.irdb.IrDeviceEntry
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.model.DeviceType
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
    val brand: String? = null,
    val testButtonLabel: String? = null,
    val ready: Boolean = false,
    // التنقّل بين أجهزة الفئة نفسها (تالي/السابق) لاختبار المرشّحين بسلاسة.
    val hasPrev: Boolean = false,
    val hasNext: Boolean = false,
    val position: Int = 0, // ترتيب 1-أساسي ضمن الفئة
    val total: Int = 0,
)

/**
 * ضبط جهاز IR محدّد (اختاره المستخدم من القاعدة). يطبّق منطق حالة الطاقة:
 * - مطفأ  → يرسل [ButtonId.POWER].
 * - مشغّل → يرسل [ButtonId.VOL_UP] أو [ButtonId.CH_UP] (تغيّر ملحوظ بلا إطفاء).
 * عند تأكيد المستخدم أن الجهاز استجاب، يُبنى [Device] جاهز يحمل مسار ملف القاعدة.
 *
 * الاكتشاف التلقائي لـ IR مستحيل فيزيائياً (أحادي الاتجاه) — لذا التأكيد بصري.
 */
@HiltViewModel
class IrSetupViewModel @Inject constructor(
    private val db: IrDatabase,
    private val registry: TransportRegistry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IrSetupUiState())
    val uiState: StateFlow<IrSetupUiState> = _uiState.asStateFlow()

    private var loadedDevice: IrDevice? = null
    private var file: String? = null
    // أجهزة الفئة نفسها بالترتيب، لتنقّل تالي/السابق.
    private var siblings: List<IrDeviceEntry> = emptyList()
    private var index: Int = -1

    /** يُحمّل الجهاز المختار من القاعدة عبر مسار ملفه. */
    fun load(irFile: String) {
        if (file == irFile) return
        file = irFile
        viewModelScope.launch {
            val ir = registry.forType(TransportType.IR)?.isAvailable() ?: false
            val all = db.index()
            val entry = all.firstOrNull { it.file == irFile }
            if (entry == null) {
                _uiState.update { it.copy(irAvailable = ir, ready = true) }
                return@launch
            }
            // قائمة المرشّحين = أجهزة الفئة نفسها بترتيب القاعدة.
            siblings = all.filter { it.category == entry.category }
            index = siblings.indexOfFirst { it.file == irFile }
            applyEntry(entry, ir)
        }
    }

    /** ينتقل للجهاز التالي/السابق في الفئة (delta = ‎+1‎ أو ‎-1‎) ويعيد التحميل فوراً. */
    fun step(delta: Int) {
        val next = index + delta
        val entry = siblings.getOrNull(next) ?: return
        index = next
        file = entry.file
        viewModelScope.launch {
            val ir = registry.forType(TransportType.IR)?.isAvailable() ?: false
            applyEntry(entry, ir)
        }
    }

    /** يحمّل جهاز الفئة المحدّد ويحدّث الحالة (يصفّر تسمية آخر اختبار + مؤشّرات التنقّل). */
    private suspend fun applyEntry(entry: IrDeviceEntry, ir: Boolean) {
        loadedDevice = db.loadDevice(entry)
        _uiState.update {
            it.copy(
                irAvailable = ir,
                brand = entry.brand,
                testButtonLabel = null,
                ready = true,
                hasPrev = index > 0,
                hasNext = index >= 0 && index < siblings.lastIndex,
                position = if (index >= 0) index + 1 else 0,
                total = siblings.size,
            )
        }
    }

    /** يرسل إشارة الاختبار حسب حالة الطاقة (مطفأ=POWER، مشغّل=VOL/CH). */
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

    /** يثبّت الجهاز كجاهز للاستخدام (بعد أن استجاب). */
    fun confirmDevice(): Device? = loadedDevice?.let { buildDevice(it) }

    /**
     * يثبّت الجهاز نفسه لكن عبر **جسر Broadlink** (تجريبي): نفس أكواد IR لكن تُرسَل عبر
     * الشبكة فيبثّها الجسر. الجسر يُكتشَف تلقائياً (لا حاجة لإدخال IP).
     */
    fun confirmViaBroadlink(): Device? = loadedDevice?.let { d ->
        buildDevice(d).copy(
            id = "bl-${d.category}-${d.brand}",
            transport = TransportType.BROADLINK,
        )
    }

    private fun buildDevice(d: IrDevice): Device = Device(
        id = "ir-${d.category}-${d.brand}",
        name = d.brand,
        type = DeviceType.TV,
        transport = TransportType.IR,
        address = null,
        // irFile = مسار ملف الجهاز في القاعدة (يطابق entry.file) ليعيد RemoteViewModel تحميله.
        metadata = mapOf("brand" to d.brand, "category" to d.category, "irFile" to (file ?: "")),
    )
}
