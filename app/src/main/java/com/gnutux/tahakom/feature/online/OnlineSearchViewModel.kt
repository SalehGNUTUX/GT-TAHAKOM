package com.gnutux.tahakom.feature.online

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gnutux.tahakom.core.irdb.online.FetchResult
import com.gnutux.tahakom.core.irdb.online.OnlineEntry
import com.gnutux.tahakom.core.irdb.online.OnlineIrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnlineSearchUiState(
    val query: String = "",
    val searched: Boolean = false,
    val results: List<OnlineEntry> = emptyList(),
    val fetchingPath: String? = null, // مسار الطقم الجاري جلبه (لإظهار مؤشّر)
)

/** أحداث لمرّة واحدة (رسائل/نجاح) لشاشة البحث الشبكي. */
sealed interface OnlineEvent {
    data class Saved(val file: String) : OnlineEvent
    data class Unsupported(val protocol: String) : OnlineEvent
    data class Error(val reason: String) : OnlineEvent
}

/**
 * بحث شبكي عن أجهزة التحكّم في قاعدة probonopd/irdb المفتوحة. البحث محلي (الفهرس مشحون)؛
 * الجلب فقط يحتاج إنترنت. عند نجاح الجلب يُبعث [OnlineEvent.Saved] بمسار الجهاز المحلي
 * الجديد ليفتحه التطبيق في شاشة الضبط مباشرة.
 */
@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    private val repo: OnlineIrRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineSearchUiState())
    val uiState: StateFlow<OnlineSearchUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<OnlineEvent?>(null)
    val events: StateFlow<OnlineEvent?> = _events.asStateFlow()
    fun consumeEvent() { _events.value = null }

    fun onQuery(q: String) {
        _uiState.update { it.copy(query = q) }
        viewModelScope.launch {
            val results = if (q.isBlank()) emptyList() else repo.search(q)
            _uiState.update { it.copy(results = results, searched = q.isNotBlank()) }
        }
    }

    fun fetch(entry: OnlineEntry) {
        if (_uiState.value.fetchingPath != null) return
        _uiState.update { it.copy(fetchingPath = entry.path) }
        viewModelScope.launch {
            val result = repo.fetchAndSave(entry)
            _uiState.update { it.copy(fetchingPath = null) }
            _events.value = when (result) {
                is FetchResult.Saved -> OnlineEvent.Saved(result.file)
                is FetchResult.Unsupported -> OnlineEvent.Unsupported(result.protocol)
                is FetchResult.Error -> OnlineEvent.Error(result.reason)
            }
        }
    }
}
