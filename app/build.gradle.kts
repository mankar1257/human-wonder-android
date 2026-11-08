import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

// Human Wonder v2: the app's architecture comes from OfflineLLM (jegly/OfflineLLM, Apache-2.0):
// Hilt, Room, Navigation Compose, the smollm native module. Plugins match OfflineLLM exactly
// (AGP 9 has Kotlin built in, so there is no kotlin-android plugin).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// v1 builds used versionCode 2 (local) or the CI run number; v2 starts at 200 so it always
// installs over them. CI passes GITHUB_RUN_NUMBER so every CI build is newer than the last.
val buildNumber = 200 + (System.getenv("GITHUB_RUN_NUMBER") ?: "0").toInt()

// ---------------------------------------------------------------------------------------------
// On-device language model, bundled inside the APK as assets/hw-core.gguf.
// It is fetched once at build time (pinned to an exact commit, SHA-256 verified) and never
// committed to git. Set HW_MODEL_FILE=/path/to/file.gguf to use a local copy instead.
// The app itself never names the model to users. ModelManager copies it out of the APK on the
// first launch (status "Orbit 412 is rising over the horizon, n%").
// ---------------------------------------------------------------------------------------------
val modelUrl = "https://huggingface.co/ggml-org/Qwen3-0.6B-GGUF/resolve/" +
    "13ac06624948e4d8589cb4b6f90100837342fcb8/Qwen3-0.6B-Q4_0.gguf"
val modelSha256 = "da2572f16c06133561ce56accaa822216f2391ef4d37fba427801cd6736417d4"
val modelBytes = 428_970_080L
val modelWorkDir = layout.buildDirectory.dir("hw-model")
val modelAssetsDir = layout.buildDirectory.dir("hw-model/assets")

val fetchModel = tasks.register("fetchModel") {
    description = "Downloads (once) and verifies the on-device model that is bundled into the APK."
    val assetFile = modelAssetsDir.map { it.file("hw-core.gguf") }
    val markerFile = modelWorkDir.map { it.file("hw-core.sha256") }
    outputs.file(assetFile)
    val url = modelUrl
    val sha = modelSha256
    val bytes = modelBytes
    doLast {
        val target = assetFile.get().asFile
        val marker = markerFile.get().asFile
        if (target.length() == bytes && marker.isFile && marker.readText().trim() == sha) {
            return@doLast
        }
        target.parentFile.mkdirs()
        val part = File(marker.parentFile, "hw-core.gguf.part")
        val local = System.getenv("HW_MODEL_FILE")?.takeIf { it.isNotBlank() }?.let { File(it) }
        if (local != null) {
            logger.lifecycle("Using local model file ${local.path}")
            local.copyTo(part, overwrite = true)
        } else {
            logger.lifecycle("Downloading the on-device model (${bytes / 1_000_000} MB, one time)...")
            val conn = URI(url).toURL().openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.inputStream.use { input -> part.outputStream().use { input.copyTo(it, 1 shl 20) } }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        part.inputStream().use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        val hex = digest.digest().joinToString("") { b -> "%02x".format(b) }
        check(hex == sha) { "Model checksum mismatch ($hex); delete app/build/hw-model and retry." }
        target.delete()
        check(part.renameTo(target)) { "Could not move the model into ${target.path}" }
        marker.writeText(sha)
    }
}
tasks.named("preBuild") { dependsOn(fetchModel) }

android {
    namespace = "ai.humanwonder.poc"
    // compileSdk 37 because the OfflineLLM library set (core-ktx 1.19, activity 1.13,
    // lifecycle 2.11, navigation 2.10, Compose BOM 2026.09) is built against it.
    compileSdk = 37

    defaultConfig {
        applicationId = "ai.humanwonder.poc"
        minSdk = 33
        targetSdk = 36
        versionCode = buildNumber
        versionName = "2.0"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        // POC signing key, committed on purpose so that every build is signed with the same key
        // and installs over the previous version (v1 used this key too). NOT for the Play Store:
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // ggml discovers its CPU backend variants (libggml-cpu-android_*.so) by scanning
            // nativeLibraryDir at runtime, so the .so files must be extracted on install.
            useLegacyPackaging = true
        }
    }

    sourceSets {
        // Release only (the APK that is installed): the debug variant, used just for the screen
        // checks, then doesn't copy the 430 MB model into every test build.
        getByName("release") {
            // A File, not a Provider: AGP 9 refuses Providers in the sourceSets DSL
            // (android.sourceset.disallowProvider). Ordering comes from preBuild -> fetchModel.
            assets.srcDir(modelAssetsDir.get().asFile)
        }
    }

    androidResources {
        // Stored uncompressed so the app can stream it straight out of the APK.
        noCompress += listOf("gguf")
    }
}

// Screen checks (app/src/test/.../screens): Robolectric draws every screen at several phone sizes
// and text sizes into app/build/screens-check; tools/build-on-mac.sh runs them after the APK.
// Set up with dynamic calls so a future AGP DSL change can never break the APK build itself.
runCatching {
    android.withGroovyBuilder {
        "testOptions" { "unitTests" { setProperty("includeAndroidResources", true) } }
    }
}.onFailure { logger.warn("Screen checks: couldn't enable Android resources in unit tests: ${it.message}") }

tasks.withType<Test>().configureEach {
    maxHeapSize = "2g"
    systemProperty("hw.screens.dir", layout.buildDirectory.dir("screens-check").get().asFile.absolutePath)
    testLogging {
        events("passed", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // Native inference module
    implementation(project(":smollm"))

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)

    // Activity & Navigation
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Security (encrypted settings)
    implementation(libs.security.crypto)

    // Core
    implementation(libs.core.ktx)

    // Serialization (conversation export)
    implementation(libs.serialization.json)

    // Screen checks only (never in the APK): Robolectric + Compose's test rule.
    testImplementation(platform(libs.compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
