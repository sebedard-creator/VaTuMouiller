import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Lecture sécurisée des clés d'API depuis local.properties
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(FileInputStream(file))
    }
}

val tomorrowKey = localProperties.getProperty("TOMORROW_IO_KEY") ?: ""
val openWeatherKey = localProperties.getProperty("OPEN_WEATHER_KEY") ?: ""

android {
    namespace = "com.sebed.vatumouiller"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sebed.vatumouiller"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Injection des clés d'API dans BuildConfig
        buildConfigField("String", "TOMORROW_IO_KEY", "\"$tomorrowKey\"")
        buildConfigField("String", "OPEN_WEATHER_KEY", "\"$openWeatherKey\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // AndroidX Core et AppCompat
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // GPS - Play Services Location
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // WorkManager pour les tâches en arrière-plan
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Réseau Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Parsing JSON - Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Jetpack Glance (Widgets Compose-like)
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.compose.runtime:runtime:1.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// Tâche stub pour contourner le bug d'importation d'Android Studio avec Gradle 9 / Kotlin DSL
if (tasks.findByName("prepareKotlinBuildScriptModel") == null) {
    tasks.register("prepareKotlinBuildScriptModel") {
        group = "help"
        description = "Stub task to bypass Android Studio sync issues"
    }
}


