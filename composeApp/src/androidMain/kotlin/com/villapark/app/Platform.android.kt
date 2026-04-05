package com.villapark.app

import android.os.Build
import okhttp3.internal.platform.Platform as OkHttpPlatform

class AndroidPlatform : OkHttpPlatform() {
    val name: String = "Android ${Build.VERSION.SDK_INT}"
}