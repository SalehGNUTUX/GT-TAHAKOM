package com.gnutux.tahakom.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gnutux.tahakom.core.irdb.IrDatabase
import com.gnutux.tahakom.core.irdb.IrDeviceEntry
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.model.DeviceType
import com.gnutux.tahakom.core.transport.TransportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddDeviceUiState(
    val query: String = "",
    /** أجهزة IR من القاعدة الحقيقية، مجمّعة حسب الفئة. */
    val irByCategory: Map<String, List<IrDeviceEntry>> = emptyMap(),
)

/**
 * يغذّي شاشة الإضافة بأجهزة IR الحقيقية من [IrDatabase] (لا قائمة وهمية).
 * يدعم البحث بالاسم/الطراز/الفئة.
 */
@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val irDb: IrDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState.asStateFlow()

    init { reload("") }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
        reload(q)
    }

    /** ترتيب عرض الفئات: التلفاز أولاً ثم أجهزة الاستقبال ثم الصوت. */
    private val categoryOrder = listOf("TV", "Cable", "Audio")

    /**
     * يستورد حزمة `.tahakom`: إن طابقت علامتها جهازاً في القاعدة المحلية اعتُمد مباشرة،
     * وإلا أُنشئ جهاز IR من معلومات الحزمة (الاسم/الفئة) ليفتح ريموته.
     */
    fun importPack(
        pack: com.gnutux.tahakom.core.share.RemotePack,
        onReady: (Device) -> Unit,
        onError: () -> Unit,
    ) {
        viewModelScope.launch {
            val match = irDb.index().firstOrNull {
                it.brand.equals(pack.brand, ignoreCase = true) ||
                    (pack.model != null && it.brand.equals(pack.model, ignoreCase = true))
            }
            if (match != null) {
                onReady(
                    Device(
                        id = "ir-${match.category}-${match.brand}",
                        name = match.brand,
                        type = DeviceType.TV,
                        transport = TransportType.IR,
                        metadata = mapOf("brand" to match.brand, "category" to match.category, "irFile" to match.file),
                    ),
                )
            } else {
                // غير مسجّل: أنشئ جهازاً من معلومات الحزمة (يُحفظ ويفتح؛ التعلّم يكمّله لاحقاً).
                val name = pack.model ?: pack.brand
                onReady(
                    Device(
                        id = "import-$name",
                        name = name,
                        type = DeviceType.TV,
                        transport = TransportType.IR,
                        metadata = mapOf("brand" to pack.brand, "category" to "TV"),
                    ),
                )
            }
        }
    }

    private fun reload(query: String) {
        viewModelScope.launch {
            val results = if (query.isBlank()) irDb.index() else irDb.search(query)
            val grouped = results.groupBy { it.category }
                .toList()
                .sortedBy { (cat, _) -> categoryOrder.indexOf(cat).let { if (it < 0) Int.MAX_VALUE else it } }
                .toMap(LinkedHashMap())
            _uiState.update { it.copy(irByCategory = grouped) }
        }
    }
}
