package com.gnutux.tahakom

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** نقطة دخول التطبيق + جذر حقن التبعيات (Hilt). */
@HiltAndroidApp
class TahakomApp : Application()
