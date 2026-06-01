package com.gnutux.tahakom.core.transport

/**
 * وسائل النقل المدعومة (أو المخطّط دعمها) في GT-TAHAKOM.
 * كل قيمة يقابلها تطبيق [Transport] واحد.
 */
enum class TransportType(val defaultPort: Int) {
    /** الأشعة تحت الحمراء عبر الباعث المدمج (ConsumerIrManager) — المرحلة 4. */
    IR(0),

    /** بروتوكول Android TV / Google TV Remote v2 (إقران TLS) — المرحلة 2. */
    ANDROID_TV(6466),

    /** Roku عبر ECP (HTTP) — المرحلة 2. */
    ROKU(8060),

    /** تلفازات Samsung الذكية (Tizen) عبر WebSocket — المرحلة 3. */
    SAMSUNG_TIZEN(8002),

    /** تلفازات LG (webOS) عبر SSAP WebSocket — المرحلة 3. */
    LG_WEBOS(3000),

    /** Sony Bravia عبر IRCC / REST — المرحلة 3. */
    SONY_BRAVIA(80),

    /** جسر WiFi-IR (Broadlink) — يتيح التحكم بأجهزة IR من أي هاتف — المرحلة 4. */
    BROADLINK(80),

    UNKNOWN(0),
}
