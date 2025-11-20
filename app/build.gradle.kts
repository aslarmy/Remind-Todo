import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Load keystore properties
val keystoreProperties = Properties()
val keystoreFile = file("keystore.properties")
if (keystoreFile.exists()) {
    keystoreProperties.load(keystoreFile.inputStream())
}

android {
    namespace = "com.sk.remindtodo"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.sk.remindtodo"
        minSdk = 24
        targetSdk = 36

        // Automatically set versionName from tag
        val gitTag = findProperty("GIT_TAG") as? String ?: "0.0.0"
        versionName = gitTag.removePrefix("v")

        // Automatically set versionCode based on commits
        val versionCodeFromGit = "git rev-list --count HEAD".runCommand()
        versionCode = versionCodeFromGit.toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ---------------------------
    // 1️⃣ Signing Configs Section
    // ---------------------------
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

// Helper function to run shell commands in Gradle
fun String.runCommand(): String =
    Runtime.getRuntime().exec(this).inputStream.bufferedReader().readText().trim()
