import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    id("com.google.gms.google-services")
}

kotlin {
    androidTarget()
    
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.core)
                implementation(libs.koin.android)
                implementation(libs.kotlinx.coroutines.android)
                // Android-specific Firebase - no platform() needed
                implementation("com.google.firebase:firebase-firestore-ktx:25.1.0")
                implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
                implementation("com.google.firebase:firebase-functions-ktx:21.0.0")
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
                // GitLive Firebase for common code
                implementation("dev.gitlive:firebase-common:2.0.0")
                implementation("dev.gitlive:firebase-firestore:2.0.0")
                implementation("dev.gitlive:firebase-functions:2.0.0")
                implementation("dev.gitlive:firebase-auth:2.0.0")
            }
        }
    }
}

android {
    namespace = "com.villapark.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.villapark.app"
        minSdk = 24
        targetSdk = 36
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
    }
}
