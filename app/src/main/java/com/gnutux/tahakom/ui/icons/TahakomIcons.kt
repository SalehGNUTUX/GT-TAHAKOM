package com.gnutux.tahakom.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * مجموعة أيقونات GT-TAHAKOM — تربط أسماء التصميم بأيقونات Material Outlined
 * (خطّية، عصرية، مجرّبة). استبدلت محلّل SVG اليدوي الذي كان ينهار على أقواس SVG.
 */
object TahakomIcons {

    fun vector(name: String): ImageVector = when (name) {
        "back" -> Icons.AutoMirrored.Outlined.ArrowBack
        "forwardNav" -> Icons.AutoMirrored.Outlined.ArrowForward
        "close" -> Icons.Outlined.Close
        "plus" -> Icons.Outlined.Add
        "check" -> Icons.Outlined.Check
        "search" -> Icons.Outlined.Search
        "more" -> Icons.Outlined.MoreVert
        "gear" -> Icons.Outlined.Settings
        "info", "info2" -> Icons.Outlined.Info
        "globe" -> Icons.Outlined.Language
        "sun" -> Icons.Outlined.LightMode
        "moon" -> Icons.Outlined.DarkMode

        "power" -> Icons.Outlined.PowerSettingsNew
        "homeBtn" -> Icons.Outlined.Home
        "caretUp" -> Icons.Outlined.KeyboardArrowUp
        "caretDown" -> Icons.Outlined.KeyboardArrowDown
        "caretLeft" -> Icons.Outlined.KeyboardArrowLeft
        "caretRight" -> Icons.Outlined.KeyboardArrowRight
        "volUp" -> Icons.AutoMirrored.Outlined.VolumeUp
        "volDown" -> Icons.AutoMirrored.Outlined.VolumeDown
        "mute" -> Icons.AutoMirrored.Outlined.VolumeOff
        "play" -> Icons.Outlined.PlayArrow
        "pause" -> Icons.Outlined.Pause
        "rewind" -> Icons.Outlined.FastRewind
        "forward" -> Icons.Outlined.FastForward
        "menu" -> Icons.Outlined.Menu
        "source" -> Icons.Outlined.Tv

        "tv" -> Icons.Outlined.Tv
        "wifi" -> Icons.Outlined.Wifi
        "ir" -> Icons.Outlined.SettingsRemote
        "bridge" -> Icons.Outlined.Router
        "link" -> Icons.Outlined.Router
        "scan" -> Icons.Outlined.Sensors
        "signal" -> Icons.Outlined.Sensors
        "shield" -> Icons.Outlined.Shield
        "delete" -> Icons.Outlined.Delete
        "share" -> Icons.Outlined.Share
        "swap" -> Icons.Outlined.ArrowDropDown

        else -> Icons.Outlined.ArrowDropDown
    }
}
