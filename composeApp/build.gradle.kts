import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget()
    
    // Desktop target disabled for now
    // jvm("desktop")
    
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.core)
                implementation(libs.koin.android)
                implementation(libs.firebase.firestore)
                implementation(libs.firebase.auth)
                implementation(libs.firebase.functions)
                implementation(libs.gitlive.firebase.firestore)
                implementation(libs.gitlive.firebase.auth)
                implementation(libs.gitlive.firebase.functions)
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.koin)
                implementation(libs.voyager.transitions)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.gitlive.firebase.common)
                implementation(libs.gitlive.firebase.firestore)
                implementation(libs.gitlive.firebase.functions)
                implementation(libs.gitlive.firebase.auth)
            }
        }
    }
}

android {
    namespace = "com.villapark.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.villapark.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(platform(libs.firebase.bom))
    }
}
