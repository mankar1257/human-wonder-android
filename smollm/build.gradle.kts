import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Native inference module, adapted from OfflineLLM's smollm module (Apache-2.0, jegly).
// Human Wonder changes: package ai.humanwonder.poc.smollm, CPU only (no Vulkan backend, so no
// glslc, no shader generator and no host C/C++ compiler are needed), NDK and CMake pinned to the
// versions tools/build-on-mac.sh installs, llama.cpp read from third_party/llama.cpp.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ai.humanwonder.poc.smollm"
    compileSdk = 37
    // Stable NDK r29, the version tools/build-on-mac.sh installs.
    ndkVersion = "29.0.14206865"

    defaultConfig {
        minSdk = 33
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            // Phones only. Every Android phone sold in recent years is arm64.
            abiFilters += "arm64-v8a"
        }
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-O3")
                arguments += listOf("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
                arguments += "-DCMAKE_BUILD_TYPE=Release"
                arguments += "-DBUILD_SHARED_LIBS=ON"
                // common drags in HTTP/download code and is not needed: the JNI wrapper uses
                // the llama.h C API directly.
                arguments += "-DLLAMA_BUILD_COMMON=OFF"
                // CPU only (Human Wonder change).
                arguments += "-DGGML_VULKAN=OFF"
                arguments += "-DGGML_NATIVE=OFF"
                // Backends stay runtime-loadable so the best CPU variant can be picked per phone.
                arguments += "-DGGML_BACKEND_DL=ON"
                // One ggml-cpu plugin per ARM feature level (armv8.0 ... armv9.2), scored against
                // the phone at load time, so dotprod/fp16/i8mm/SVE kernels run where available.
                arguments += "-DGGML_CPU_ALL_VARIANTS=ON"
            }
        }
    }

    buildTypes {
        // Native code is always compiled with -O3 (CMAKE_BUILD_TYPE=Release above).
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            // The CMake tools/build-on-mac.sh installs into the SDK.
            version = "3.31.6"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.coroutines.core)
}
