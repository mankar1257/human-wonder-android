import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// CI passes GITHUB_RUN_NUMBER so every build can be installed over the previous one.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "ai.humanwonder.poc"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.humanwonder.poc"
        minSdk = 28
        targetSdk = 36
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        // POC signing key, committed on purpose so that every CI build is signed with the
        // same key and installs over the previous version. NOT for the Play Store:
        // set HW_KEYSTORE / HW_KEYSTORE_PASSWORD / HW_KEY_ALIAS / HW_KEY_PASSWORD
        // (e.g. from GitHub secrets) to sign with a private key instead.
        create("poc") {
            // Unset GitHub secrets arrive as empty strings, so treat blank as "not provided".
            fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }
            storeFile = file(env("HW_KEYSTORE") ?: "../keystore/poc-release.jks")
            storePassword = env("HW_KEYSTORE_PASSWORD") ?: "humanwonder-poc"
            keyAlias = env("HW_KEY_ALIAS") ?: "humanwonder-poc"
            keyPassword = env("HW_KEY_PASSWORD") ?: "humanwonder-poc"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("poc")
        }
    }

    packaging {
        jniLibs {
            // llama.cpp loads its CPU backend variants with dlopen() from the app's
            // native library directory, so the .so files must be extracted on install.
            useLegacyPackaging = true
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":lib"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
}
