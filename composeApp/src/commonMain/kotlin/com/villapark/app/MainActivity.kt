// ============================================================
// FILE: composeApp/src/androidMain/kotlin/com/villapark/app/MainActivity.kt
// ACTION: REPLACE entire file
// ============================================================
package com.villapark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.villapark.app.domain.di.appModules
import com.villapark.app.presentation.navigation.AppNavigation
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startKoin {
            androidContext(this@MainActivity)

            modules(appModules)
        }
        setContent {
            AppNavigation()
        }
    }
}