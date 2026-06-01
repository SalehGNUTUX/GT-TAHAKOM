package com.gnutux.tahakom.core.discovery

import com.gnutux.tahakom.core.model.Device

/** يحوّل جهازاً مكتشَفاً إلى [Device] قابل للتحكّم/الحفظ. */
fun DiscoveredDevice.toDevice(): Device = Device(
    id = "$host:$port",
    name = name,
    type = type,
    transport = transport,
    address = host,
    metadata = buildMap {
        brand?.let { put("brand", it) }
        model?.let { put("model", it) }
        put("port", port.toString())
    },
)
