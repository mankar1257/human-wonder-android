import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// CI passes GITHUB_RUN_NUMBER so every build can be installed over the previous one.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "2").toInt()

// ---------------------------------------------------------------------------------------------
// On-device language model, bundled inside the APK as assets/hw-core.gguf.
// It is fetched once at build time (pinned to an exact commit, SHA-256 verified) and never
// committed to git. Set HW_MODEL_FILE=/path/to/file.gguf to use a local copy instead.
// The app itself never names the model to users.
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
            val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.inputStream.use { input -> part.outputStream().use { input.copyTo(it, 1 shl 20) } }
        }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        part.inputStream().use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        val hex = digest.digest().joinToString("") { "%02x".format(it) }
        check(hex == sha) { "Model checksum mismatch ($hex); delete app/build/hw-model and retry." }
        target.delete()
        check(part.renameTo(target)) { "Could not move the model into ${target.path}" }
        marker.writeText(sha)
    }
}
tasks.named("preBuild") { dependsOn(fetchModel) }

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

    sourceSets {
        getByName("main") {
            assets.srcDir(modelAssetsDir)
        }
    }

    androidResources {
        // Stored uncompressed so the app can stream it straight out of the APK.
        noCompress += listOf("gguf")
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
