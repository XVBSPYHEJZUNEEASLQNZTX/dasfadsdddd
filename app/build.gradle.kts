plugins {
    id("com.android.application")
}

android {
    namespace = "app.ntc.lite"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.ntc.lite"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Подпись debug-ключом: APK ставится на телефон напрямую (для личного использования).
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Никаких зависимостей: ни AndroidX, ни сторонних библиотек.
