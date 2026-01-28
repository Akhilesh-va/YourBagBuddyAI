import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.yourbagbuddy"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.yourbagbuddy"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API keys are provided via Gradle properties and exposed only through BuildConfig.
        // Never hardcode real keys in source. Define ZAI_API_KEY in local.properties or
        // your CI environment and keep it out of version control.
        // Read the API keys and backend base URL from local.properties (or other Gradle properties).
        // Using gradleLocalProperties ensures values from local.properties are loaded,
        // which are NOT automatically exposed as project properties.
        // In recent AGP versions, gradleLocalProperties requires the ProviderFactory (`providers`) argument.
        val zaiApiKey = gradleLocalProperties(rootDir, providers).getProperty("ZAI_API_KEY", "")
        val openRouterApiKey = gradleLocalProperties(rootDir, providers).getProperty("OPENROUTER_API_KEY", "")
        val googleWebClientId = gradleLocalProperties(rootDir, providers).getProperty("GOOGLE_WEB_CLIENT_ID", "")
        // Backend base URL used by the app to talk to YourBagBuddy backend.
        // You can override this per-environment via local.properties or CI env vars.
        val apiBaseUrl = gradleLocalProperties(rootDir, providers)
            .getProperty(
                "API_BASE_URL",
                "https://your-back-buddy-backend.vercel.app/"
            )
        buildConfigField("String", "ZAI_API_KEY", "\"$zaiApiKey\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterApiKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        // Required because we define custom BuildConfig fields (e.g. ZAI_API_KEY)
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    
    // Firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Lifecycle + ViewModel + Compose State
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    
    // Gson for serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Networking: Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Lottie animations for Compose
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}