import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

//
// ────────────────────────────────────────────────
//  🔐 Load Keystore Properties (if exists)
// ────────────────────────────────────────────────
//
val keystoreProperties = Properties()
val keystoreFile = file("keystore.properties")

if (keystoreFile.exists()) {
    keystoreProperties.load(keystoreFile.inputStream())
}

//
// ────────────────────────────────────────────────
//  🏷 Git Versioning (Safe for CI + Local)
// ────────────────────────────────────────────────
//
fun String.safeRunCommand(): String? {
    return try {
        val parts = this.split(" ")
        val process = ProcessBuilder(parts)
            .redirectErrorStream(true)
            .start()

        process.inputStream.bufferedReader().readText()
            .trim()
            .ifBlank { null }

    } catch (e: Exception) {
        null
    }
}

val gitTag = "git describe --tags --abbrev=0".safeRunCommand()
    ?: project.findProperty("VERSION_NAME")?.toString()
    ?: "0.0.0"

val versionNameComputed = gitTag.removePrefix("v")

val gitCommitCount = "git rev-list --count HEAD".safeRunCommand()
    ?: project.findProperty("VERSION_CODE")?.toString()
    ?: "1"

val versionCodeComputed = gitCommitCount.toInt()

//
// ────────────────────────────────────────────────
//  🧩 Android Block
// ────────────────────────────────────────────────
//
android {
    namespace = "com.sk.remindtodo"

    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.sk.remindtodo"
        minSdk = 24
        targetSdk = 36

        // Auto Versioning
        versionName = versionNameComputed
        versionCode = versionCodeComputed

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    //
    // ────────────────────────────────
    //  🔐 Signing Config
    // ────────────────────────────────
    //
    signingConfigs {
        create("release") {
            if (keystoreFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    //
    // ────────────────────────────────
    //  🏗 Build Types
    // ────────────────────────────────
    //
    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    //
    // ────────────────────────────────
    //  ⚙ Kotlin / Java Options
    // ────────────────────────────────
    //
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

//
// ────────────────────────────────────────────────
//  📦 Dependencies
// ────────────────────────────────────────────────
//
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

//
// ────────────────────────────────────────────────
//  🧪 Task: Print Version Info
// ────────────────────────────────────────────────
//
tasks.register("printVersion") {
    doLast {
        println("=== Version Info ===")
        println("versionName = ${android.defaultConfig.versionName}")
        println("versionCode = ${android.defaultConfig.versionCode}")
        println("====================")

        // Example:
        // ./gradlew printVersion -PVERSION_NAME=v1.2.0 -PVERSION_CODE=120
    }
}
