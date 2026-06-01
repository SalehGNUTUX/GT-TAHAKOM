package com.gnutux.tahakom.core.irdb

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * يخزّن الريموتات المُتعلَّمة/المخصّصة من المستخدم (مثل Unionaire غير الموثّقة)
 * كأجهزة IR محلية في ملفات JSON ضمن التخزين الخاص بالتطبيق.
 *
 * يكمّل قاعدة assets/irdb للقراءة فقط: هنا أجهزة المستخدم القابلة للكتابة.
 * كل جهاز = نفس صيغة IrDevice (أزرار بمعرّف دلالي + كود Pronto).
 */
class LearnedRemoteStore(private val context: Context) {

    private val dir = context.filesDir.resolve("learned_remotes").apply { mkdirs() }

    /** يحفظ/يحدّث ريموتاً متعلَّماً. المعرّف من العلامة (مثل "unionaire-tv"). */
    fun save(device: IrDevice) {
        val o = JSONObject()
        o.put("category", device.category)
        o.put("brand", device.brand)
        o.put("model", device.model)
        o.put("freq", device.freq)
        val arr = JSONArray()
        device.buttons.forEach { b ->
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("code", b.code)
                    .put("freq", b.freq)
                    .apply { b.label?.let { put("label", it) } },
            )
        }
        o.put("buttons", arr)
        file(device.brand).writeText(o.toString())
    }

    /** يقرأ كل الريموتات المتعلَّمة. */
    fun all(): List<IrDevice> = dir.listFiles { f -> f.extension == "json" }
        ?.mapNotNull { runCatching { parse(it.readText()) }.getOrNull() }
        ?: emptyList()

    fun load(brand: String): IrDevice? =
        file(brand).takeIf { it.exists() }?.let { runCatching { parse(it.readText()) }.getOrNull() }

    fun delete(brand: String) { file(brand).delete() }

    private fun file(brand: String) = dir.resolve("${slug(brand)}.json")
    private fun slug(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private fun parse(text: String): IrDevice {
        val o = JSONObject(text)
        val arr = o.getJSONArray("buttons")
        val buttons = (0 until arr.length()).map { i ->
            val b = arr.getJSONObject(i)
            IrButton(
                id = b.optString("id", "UNKNOWN"),
                code = b.getString("code"),
                freq = b.optInt("freq", o.optInt("freq", 38000)),
                label = if (b.has("label")) b.getString("label") else null,
            )
        }
        return IrDevice(
            category = o.optString("category", "TV"),
            brand = o.getString("brand"),
            model = o.optString("model", o.getString("brand")),
            freq = o.optInt("freq", 38000),
            buttons = buttons,
        )
    }
}
