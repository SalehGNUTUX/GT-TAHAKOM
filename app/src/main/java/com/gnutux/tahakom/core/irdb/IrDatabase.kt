package com.gnutux.tahakom.core.irdb

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * قاعدة بيانات الأشعة تحت الحمراء المحلية — تقرأ من `assets/irdb/`.
 *
 * **أوفلاين بالكامل، بلا إنترنت** (انظر docs/DATABASE.md). الفهرس يُحمَّل مرة
 * ويُخزَّن، وملفات الأجهزة تُحمَّل عند الطلب فقط (كسولة) لتوفير الذاكرة.
 */
class IrDatabase(private val context: Context) {

    private val mutex = Mutex()
    @Volatile private var index: List<IrDeviceEntry>? = null

    /** فهرس كل الأجهزة (يُحمَّل مرة واحدة). */
    suspend fun index(): List<IrDeviceEntry> {
        index?.let { return it }
        return mutex.withLock {
            index ?: loadIndex().also { index = it }
        }
    }

    /** الفئات المتاحة (TV, Cable, Audio...). */
    suspend fun categories(): List<String> =
        index().map { it.category }.distinct().sorted()

    /** أجهزة فئة معيّنة. */
    suspend fun devicesIn(category: String): List<IrDeviceEntry> =
        index().filter { it.category == category }

    /** بحث بالاسم/العلامة/الطراز (غير حسّاس لحالة الأحرف). */
    suspend fun search(query: String): List<IrDeviceEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return index()
        return index().filter {
            it.brand.lowercase().contains(q) ||
                it.model.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
        }
    }

    /** يحمّل جهازاً كاملاً بأزراره من ملفه. */
    suspend fun loadDevice(entry: IrDeviceEntry): IrDevice = withContext(Dispatchers.IO) {
        val text = context.assets.open("irdb/${entry.file}").bufferedReader().use { it.readText() }
        parseDevice(text)
    }

    private fun loadIndex(): List<IrDeviceEntry> {
        val text = context.assets.open("irdb/index.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val arr = root.getJSONArray("devices")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            IrDeviceEntry(
                category = o.getString("category"),
                brand = o.getString("brand"),
                model = o.optString("model", o.getString("brand")),
                file = o.getString("file"),
                freq = o.optInt("freq", 0),
                buttons = o.optInt("buttons", 0),
            )
        }
    }

    private fun parseDevice(text: String): IrDevice {
        val o = JSONObject(text)
        val arr = o.getJSONArray("buttons")
        val buttons = (0 until arr.length()).map { i ->
            val b = arr.getJSONObject(i)
            IrButton(
                id = b.optString("id", "UNKNOWN"),
                code = b.getString("code"),
                freq = b.optInt("freq", o.optInt("freq", 0)),
                label = if (b.has("label")) b.getString("label") else null,
            )
        }
        return IrDevice(
            category = o.getString("category"),
            brand = o.getString("brand"),
            model = o.optString("model", o.getString("brand")),
            freq = o.optInt("freq", 0),
            buttons = buttons,
        )
    }
}
