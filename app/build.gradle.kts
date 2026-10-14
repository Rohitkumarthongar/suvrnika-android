plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val apiUrl = (project.findProperty("suvrnikaApiUrl") as String? ?: "http://10.0.2.2:3000/").let { if (it.endsWith("/")) it else "$it/" }
// Set by the release workflow (.github/workflows/release.yml); local builds stay at 1 so any published release is newer.
val appVersionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
val appVersionName = project.findProperty("appVersionName") as String? ?: "1.0.0"
// Public GitHub repository whose releases the app offers as updates.
val updateRepo = project.findProperty("updateRepo") as String? ?: "Rohitkumarthongar/suvrnika-android"
// Release signing from the environment (the workflow decodes the keystore from a secret). Every release must be
// signed with the same key, or Android refuses to install it over the app already on the phone.
val keystorePath: String? = System.getenv("SUVRNIKA_KEYSTORE_FILE")

android {
    namespace = "com.suvrnika.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.suvrnika.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
        buildConfigField("String", "UPDATE_REPO", "\"$updateRepo\"")
    }

    signingConfigs {
        if (keystorePath != null) create("release") {
            storeFile = file(keystorePath)
            storePassword = System.getenv("SUVRNIKA_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("SUVRNIKA_KEY_ALIAS")
            keyPassword = System.getenv("SUVRNIKA_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
            // Release builds must point at an https:// server (cleartext is only allowed in debug builds).
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("com.razorpay:checkout:1.6.41")
}
