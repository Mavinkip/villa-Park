package com.villapark.app

import android.os.Build
import okhttp3.internal.platform.Platform

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

