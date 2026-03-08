import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mitube.mlc"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mitube.mlc"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }
        val geminiApiKeyStr = localProperties.getProperty("geminiApiKey") ?: "YOUR_GEMINI_API_KEY_HERE"

        manifestPlaceholders += mapOf(
            "appAuthRedirectScheme" to "com.mitube.mlc",
            "geminiApiKey" to geminiApiKeyStr
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    // Google Sign-In & YouTube API Oauth
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    // AppAuth for OAuth 2.0 Web Flow
    implementation("net.openid:appauth:0.11.1")
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // OkHttp for API requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Glide for Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}