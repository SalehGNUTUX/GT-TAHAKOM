package com.gnutux.tahakom.core.share

import org.json.JSONArray
import org.json.JSONObject

/**
 * تحويل [RemotePack] من/إلى JSON.
 *
 * يستخدم `org.json` المدمج في أندرويد (بلا تبعية خارجية). الإخراج نصّي
 * مقروء، يُكتب في ملف بامتداد `.tahakom` ويُقرأ عند فتحه/استيراده.
 */
object RemotePackCodec {

    fun encode(pack: RemotePack): String {
        val root = JSONObject()
        root.put("schema", pack.schema)
        root.put("scope", pack.scope.name)
        root.put("brand", pack.brand)
        pack.model?.let { root.put("model", it) }
        pack.description?.let { root.put("description", it) }
        pack.author?.let { root.put("author", it) }
        val remotes = JSONArray()
        pack.remotesJson.forEach { remotes.put(JSONObject(it)) }
        root.put("remotes", remotes)
        return root.toString(2)
    }

    /** يُرجع null إذا كان النص غير صالح أو إصداره غير مدعوم. */
    fun decode(text: String): RemotePack? {
        return try {
            val root = JSONObject(text)
            val schema = root.optInt("schema", RemotePack.CURRENT_SCHEMA)
            if (schema > RemotePack.CURRENT_SCHEMA) return null // أحدث من قدرتنا

            val remotes = root.optJSONArray("remotes") ?: JSONArray()
            val remotesJson = (0 until remotes.length()).map { remotes.getJSONObject(it).toString() }

            RemotePack(
                schema = schema,
                scope = runCatching { PackScope.valueOf(root.getString("scope")) }
                    .getOrDefault(PackScope.BRAND),
                brand = root.getString("brand"),
                model = root.optStringOrNull("model"),
                description = root.optStringOrNull("description"),
                author = root.optStringOrNull("author"),
                remotesJson = remotesJson,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
