package com.gnutux.tahakom.core.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gnutux.tahakom.core.model.Device
import com.gnutux.tahakom.core.model.DeviceType
import com.gnutux.tahakom.core.transport.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gt_tahakom")

/**
 * مستودع الأجهزة التي اعتمدها المستخدم — يحفظها ويستعيدها ويرتّبها ويحذفها.
 *
 * يخزّن قائمة [Device] كـ JSON في DataStore (أوفلاين، خفيف، يكفي لقائمة أجهزة).
 * الترتيب في القائمة هو ترتيب العرض، فإعادة الترتيب = إعادة كتابة القائمة.
 */
class SavedDevicesRepository(private val context: Context) {

    private val key = stringPreferencesKey("saved_devices")
    private val onboardingKey = booleanPreferencesKey("onboarding_done")
    private val themeKey = stringPreferencesKey("theme_mode") // "system" | "light" | "dark"

    /** هل أنهى المستخدم شاشة الترحيب؟ */
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[onboardingKey] ?: false }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[onboardingKey] = true }
    }

    /** يعيد ضبط شاشة الترحيب لتُعرَض من جديد (زر في الإعدادات). */
    suspend fun resetOnboarding() {
        context.dataStore.edit { it[onboardingKey] = false }
    }

    /** وضع السمة: system / light / dark. */
    val themeMode: Flow<String> = context.dataStore.data.map { it[themeKey] ?: "system" }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[themeKey] = mode }
    }

    /** تدفّق الأجهزة المحفوظة (يتحدّث تلقائياً عند أي تغيير). */
    val devices: Flow<List<Device>> = context.dataStore.data.map { prefs ->
        decode(prefs[key] ?: "[]")
    }

    /** يضيف جهازاً (أو يحدّثه إن كان موجوداً بنفس المعرّف) في **رأس** القائمة (الأحدث أولاً). */
    suspend fun add(device: Device) = update { current ->
        listOf(device) + current.filterNot { it.id == device.id }
    }

    suspend fun remove(deviceId: String) = update { current ->
        current.filterNot { it.id == deviceId }
    }

    /** يعيد ترتيب القائمة بالكامل (بعد سحب/إفلات في الواجهة). */
    suspend fun reorder(ordered: List<Device>) = update { ordered }

    private suspend fun update(transform: (List<Device>) -> List<Device>) {
        context.dataStore.edit { prefs ->
            val current = decode(prefs[key] ?: "[]")
            prefs[key] = encode(transform(current))
        }
    }

    // --- تحويل JSON ---

    private fun encode(devices: List<Device>): String {
        val arr = JSONArray()
        devices.forEach { d ->
            val o = JSONObject()
            o.put("id", d.id)
            o.put("name", d.name)
            o.put("type", d.type.name)
            o.put("transport", d.transport.name)
            d.address?.let { o.put("address", it) }
            val meta = JSONObject()
            d.metadata.forEach { (k, v) -> meta.put(k, v) }
            o.put("metadata", meta)
            arr.put(o)
        }
        return arr.toString()
    }

    private fun decode(text: String): List<Device> = runCatching {
        val arr = JSONArray(text)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val meta = o.optJSONObject("metadata") ?: JSONObject()
            Device(
                id = o.getString("id"),
                name = o.getString("name"),
                type = runCatching { DeviceType.valueOf(o.getString("type")) }
                    .getOrDefault(DeviceType.OTHER),
                transport = runCatching { TransportType.valueOf(o.getString("transport")) }
                    .getOrDefault(TransportType.UNKNOWN),
                address = if (o.has("address")) o.getString("address") else null,
                metadata = meta.keys().asSequence().associateWith { meta.getString(it) },
            )
        }
    }.getOrDefault(emptyList())
}
