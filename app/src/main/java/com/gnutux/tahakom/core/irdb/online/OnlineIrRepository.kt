package com.gnutux.tahakom.core.irdb.online

import android.content.Context
import com.gnutux.tahakom.core.irdb.IrDatabase
import com.gnutux.tahakom.core.irdb.IrDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** مدخل في فهرس البحث الشبكي (online_index.json) — طقم أكواد متاح في probonopd/irdb. */
data class OnlineEntry(
    val brand: String,
    val category: String,
    val type: String,
    val path: String,      // نسبي تحت codes/ (مثل "Coby/TV/0,127.csv")
    val protocol: String,
    val functions: Int,
    val supported: Boolean,
) {
    /** وسم الطقم المختصر (اسم الملف بلا امتداد، مثل "0,127"). */
    val tag: String get() = path.substringAfterLast('/').removeSuffix(".csv")
    /** اسم عرض فريد يميّز أطقم العلامة نفسها (للحفظ المحلي ولشاشة الضبط). */
    val displayName: String get() = "$brand ($type·$tag)"
}

/** نتيجة محاولة جلب طقم وتحويله. */
sealed interface FetchResult {
    /** نجح: حُوّل وحُفظ محلياً؛ [file] يطابق مدخل القاعدة (بادئة learned:). */
    data class Saved(val device: IrDevice, val file: String) : FetchResult
    /** البروتوكول غير مدعوم بعد على الهاتف. */
    data class Unsupported(val protocol: String) : FetchResult
    /** فشل (شبكة/تحويل بلا أزرار صالحة). */
    data class Error(val reason: String) : FetchResult
}

/**
 * البحث الشبكي عن أجهزة التحكّم في قاعدة probonopd/irdb (الطبقة «التوسعية»، docs/DATABASE.md).
 *
 * **هجين، أوفلاين-أولاً:** الفهرس (online_index.json) مشحون كأصل فالبحث بالعلامة أوفلاين؛
 * الإنترنت يُستخدم فقط لتنزيل أكواد الطقم المختار من raw.githubusercontent.com (CDN، بلا
 * حدّ معدّل API)، ثم يُحوَّل على الهاتف عبر [IrCodeConverter] ويُحفظ كجهاز محلي قابل للاستخدام.
 */
class OnlineIrRepository(
    private val context: Context,
    private val db: IrDatabase,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val mutex = Mutex()
    @Volatile private var cached: List<OnlineEntry>? = null

    // بعض المسارات تحوي مسافات («Classe Audio») أو فواصل («0,127») — تُرمَّز بالقطع لا بالسَّلسلة.
    private fun rawUrl(path: String): HttpUrl = HttpUrl.Builder()
        .scheme("https").host("raw.githubusercontent.com")
        .addPathSegments("probonopd/irdb/master/codes")
        .addPathSegments(path)
        .build()

    /** يحمّل الفهرس المشحون (أوفلاين). */
    suspend fun index(): List<OnlineEntry> = withContext(Dispatchers.IO) {
        cached ?: mutex.withLock { cached ?: loadIndex().also { cached = it } }
    }

    /**
     * بحث بالعلامة. يُرجع الأطقم المطابقة، **المدعومة أولاً** ثم الأكثر أزراراً.
     * يستثني العلامات الموجودة فعلاً محلياً اختياريّاً عبر [excludeLocalBrands].
     */
    suspend fun search(query: String, excludeLocalBrands: Set<String> = emptySet()): List<OnlineEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return index()
            .filter { it.brand.lowercase().contains(q) && it.brand.lowercase() !in excludeLocalBrands }
            .sortedWith(compareByDescending<OnlineEntry> { it.supported }.thenByDescending { it.functions })
    }

    /** يجلب طقماً عبر الإنترنت، يحوّله على الهاتف، ويحفظه كجهاز محلي. */
    suspend fun fetchAndSave(entry: OnlineEntry): FetchResult = withContext(Dispatchers.IO) {
        if (!entry.supported) return@withContext FetchResult.Unsupported(entry.protocol)
        val text = try {
            val req = Request.Builder().url(rawUrl(entry.path)).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext FetchResult.Error("HTTP ${resp.code}")
                resp.body?.string() ?: return@withContext FetchResult.Error("empty body")
            }
        } catch (e: Exception) {
            return@withContext FetchResult.Error(e.message ?: "network error")
        }
        val buttons = IrCodeConverter.convert(text).filter { it.id != "UNKNOWN" }
        if (buttons.isEmpty()) return@withContext FetchResult.Error("no usable buttons")
        val freq = buttons.firstOrNull()?.freq ?: 38000
        val device = IrDevice(
            category = if (entry.category == "Other") "TV" else entry.category,
            brand = entry.displayName,
            model = entry.displayName,
            freq = freq,
            buttons = buttons,
        )
        db.saveLearned(device)
        FetchResult.Saved(device, "learned:${device.brand}")
    }

    private fun loadIndex(): List<OnlineEntry> {
        val text = context.assets.open("online_index.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("devices")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            OnlineEntry(
                brand = o.getString("brand"),
                category = o.optString("category", "Other"),
                type = o.optString("type", ""),
                path = o.getString("path"),
                protocol = o.optString("protocol", ""),
                functions = o.optInt("functions", 0),
                supported = o.optBoolean("supported", false),
            )
        }
    }
}
