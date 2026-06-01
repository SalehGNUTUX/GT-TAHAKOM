package com.gnutux.tahakom.core.model

/**
 * تخطيطات ريموت جاهزة. مبدئياً تخطيط تلفاز قياسي يغطّي أهم الأزرار،
 * على غرار الريموت الافتراضي في IRRemote. تُستعمل حين لا يملك الجهاز
 * ريموتاً مخصّصاً محفوظاً.
 */
object RemoteLayouts {

    /** ريموت تلفاز قياسي: طاقة، تنقّل، صوت، قنوات، وسائط. */
    fun standardTv(deviceId: String): Remote = Remote(
        id = "std-tv-$deviceId",
        name = "Standard TV",
        deviceId = deviceId,
        buttons = listOf(
            btn("power", ButtonId.POWER, "⏻"),
            btn("home", ButtonId.HOME, "⌂"),
            btn("back", ButtonId.BACK, "↩"),

            btn("up", ButtonId.NAV_UP, "▲"),
            btn("down", ButtonId.NAV_DOWN, "▼"),
            btn("left", ButtonId.NAV_LEFT, "◀"),
            btn("right", ButtonId.NAV_RIGHT, "▶"),
            btn("ok", ButtonId.NAV_OK, "OK"),

            btn("vol_up", ButtonId.VOL_UP, "+"),
            btn("vol_down", ButtonId.VOL_DOWN, "−"),
            btn("mute", ButtonId.MUTE, "🔇"),

            btn("ch_up", ButtonId.CH_UP, "CH+"),
            btn("ch_down", ButtonId.CH_DOWN, "CH−"),

            btn("rwd", ButtonId.RWD, "⏪"),
            btn("play", ButtonId.PLAY, "⏯"),
            btn("ffwd", ButtonId.FFWD, "⏩"),

            btn("info", ButtonId.INFO, "ℹ"),
            btn("menu", ButtonId.MENU, "☰"),
            btn("source", ButtonId.SOURCE, "⮂"),
        ),
    )

    private fun btn(id: String, button: ButtonId, label: String) =
        RemoteButton(id = id, label = label, command = Command.Key(button))
}
