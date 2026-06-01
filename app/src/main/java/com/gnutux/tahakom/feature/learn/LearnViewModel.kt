package com.gnutux.tahakom.feature.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gnutux.tahakom.core.irdb.IrButton
import com.gnutux.tahakom.core.irdb.IrDatabase
import com.gnutux.tahakom.core.irdb.IrDevice
import com.gnutux.tahakom.core.irdb.Pronto
import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.model.DeviceType
import com.gnutux.tahakom.core.transport.TransportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** زر قيد التعلّم: المعرّف الدلالي + الكود المُدخَل (Pronto) + هل صالح. */
data class LearnEntry(
    val button: ButtonId,
    val labelRes: Int,
    val code: String = "",
)

data class LearnUiState(
    val brand: String = "",
    val entries: List<LearnEntry> = emptyList(),
    val savedOk: Boolean = false,
    val error: String? = null,
)

/**
 * تعلّم ريموت بالإدخال اليدوي: المستخدم يُدخل كود Pronto لكل زر (يجده أونلاين أو
 * من ريموت آخر أو حزمة مشتركة)، فيُحفظ كجهاز IR مخصّص يظهر في القائمة ويعمل.
 *
 * يحلّ دعم الأجهزة غير الموثّقة (مثل Unionaire) بلا عتاد إضافي.
 */
@HiltViewModel
class LearnViewModel @Inject constructor(
    private val irDb: IrDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LearnUiState(entries = DEFAULT_BUTTONS.map { (id, res) -> LearnEntry(id, res) }),
    )
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    fun setBrand(name: String) = _uiState.update { it.copy(brand = name, error = null) }

    fun setCode(button: ButtonId, code: String) = _uiState.update { st ->
        st.copy(entries = st.entries.map { if (it.button == button) it.copy(code = code) else it }, error = null)
    }

    /** يتحقّق ويحفظ. يقبل الأزرار ذات الأكواد الصالحة فقط (Pronto قابل للتحليل). */
    fun save(): Device? {
        val st = _uiState.value
        val brand = st.brand.trim()
        if (brand.isEmpty()) {
            _uiState.update { it.copy(error = "empty_brand") }
            return null
        }
        val valid = st.entries.filter { it.code.isNotBlank() && Pronto.parse(it.code) != null }
        if (valid.isEmpty()) {
            _uiState.update { it.copy(error = "no_valid_code") }
            return null
        }
        val device = IrDevice(
            category = "TV",
            brand = brand,
            model = brand,
            freq = valid.firstNotNullOfOrNull { Pronto.parse(it.code)?.frequencyHz } ?: 38000,
            buttons = valid.map { IrButton(id = it.button.name, code = it.code, freq = 38000) },
        )
        irDb.saveLearned(device)
        _uiState.update { it.copy(savedOk = true, error = null) }
        return Device(
            id = "ir-TV-$brand",
            name = brand,
            type = DeviceType.TV,
            transport = TransportType.IR,
            address = null,
            metadata = mapOf("brand" to brand, "category" to "TV", "irFile" to "learned:$brand"),
        )
    }

    companion object {
        /** الأزرار الأساسية المقترحة للتعلّم (يكفي إدخال ما يلزم منها). */
        val DEFAULT_BUTTONS = listOf(
            ButtonId.POWER to com.gnutux.tahakom.R.string.learn_btn_power,
            ButtonId.VOL_UP to com.gnutux.tahakom.R.string.learn_btn_vol_up,
            ButtonId.VOL_DOWN to com.gnutux.tahakom.R.string.learn_btn_vol_down,
            ButtonId.MUTE to com.gnutux.tahakom.R.string.learn_btn_mute,
            ButtonId.CH_UP to com.gnutux.tahakom.R.string.learn_btn_ch_up,
            ButtonId.CH_DOWN to com.gnutux.tahakom.R.string.learn_btn_ch_down,
            ButtonId.NAV_UP to com.gnutux.tahakom.R.string.learn_btn_up,
            ButtonId.NAV_DOWN to com.gnutux.tahakom.R.string.learn_btn_down,
            ButtonId.NAV_LEFT to com.gnutux.tahakom.R.string.learn_btn_left,
            ButtonId.NAV_RIGHT to com.gnutux.tahakom.R.string.learn_btn_right,
            ButtonId.NAV_OK to com.gnutux.tahakom.R.string.learn_btn_ok,
            ButtonId.BACK to com.gnutux.tahakom.R.string.learn_btn_back,
            ButtonId.HOME to com.gnutux.tahakom.R.string.learn_btn_home,
            ButtonId.MENU to com.gnutux.tahakom.R.string.learn_btn_menu,
            ButtonId.SOURCE to com.gnutux.tahakom.R.string.learn_btn_source,
        )
    }
}
