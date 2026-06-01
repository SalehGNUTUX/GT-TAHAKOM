package com.gnutux.tahakom.core.discovery

import android.content.Context
import android.net.wifi.WifiManager

/**
 * بعض الأجهزة تتجاهل حزم multicast (mDNS/SSDP) ما لم يُمسَك MulticastLock.
 * يُمسَك أثناء المسح ويُحرَّر بعده لتوفير الطاقة.
 */
class MulticastLockHolder(context: Context) {

    private val wifi = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var lock: WifiManager.MulticastLock? = null

    fun acquire() {
        if (lock?.isHeld == true) return
        lock = wifi?.createMulticastLock("gt-tahakom-discovery")?.apply {
            setReferenceCounted(false)
            runCatching { acquire() }
        }
    }

    fun release() {
        runCatching { lock?.takeIf { it.isHeld }?.release() }
        lock = null
    }
}
