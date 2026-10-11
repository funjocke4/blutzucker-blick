plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "de.blutzuckerblick"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.blutzuckerblick"
        minSdk = 26
        targetSdk = 35
        // Only 64-bit builds; image reading itself is pure Kotlin, without native Tesseract.
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        versionCode = 46
        versionName = "1.3.4"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.camera:camera-core:1.5.3")
    implementation("androidx.camera:camera-camera2:1.5.3")
    implementation("androidx.camera:camera-lifecycle:1.5.3")
    implementation("androidx.camera:camera-view:1.5.3")
}
