import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// On-device inference library: llama.cpp (git submodule, pinned to v0.9.0) plus
// Arm's "aichat" JNI wrapper from llama.cpp's examples/llama.android, with small,
// commented changes (see "Human Wonder change" in the sources).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.arm.aichat"
    compileSdk = 36
    // Stable NDK r29 (the same version llama.cpp's own Android CI installs). Upstream's example
    // used 29.0.13113456, a pre-release that CI machines may not be able to download.
    ndkVersion = "29.0.14206865"

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")

        ndk {
            // Phones only. Every Android phone sold in recent years is arm64.
            abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DBUILD_SHARED_LIBS=ON",
                    "-DLLAMA_BUILD_APP=OFF",
                    "-DLLAMA_BUILD_COMMON=ON",
                    "-DLLAMA_OPENSSL=OFF",
                    "-DGGML_NATIVE=OFF",
                    // Build every Arm CPU variant (armv8.0 ... armv9.2) and pick the best
                    // one for the phone at runtime, so old and new phones both work.
                    "-DGGML_BACKEND_DL=ON",
                    "-DGGML_CPU_ALL_VARIANTS=ON",
                    "-DGGML_LLAMAFILE=OFF",
                )
            }
        }
    }

    buildTypes {
        // Native code is always compiled with -O2/-O3 (CMAKE_BUILD_TYPE=Release above),
        // so even debug APKs run the model at full speed.
        release {
            isMinifyEnabled = false
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
