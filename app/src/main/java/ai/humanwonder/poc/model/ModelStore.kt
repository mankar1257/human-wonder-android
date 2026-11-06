package ai.humanwonder.poc.model

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * The one model this POC ships with.
 *
 * Qwen3-0.6B (Apache-2.0), 4-bit Q4_0 GGUF published by ggml-org (the llama.cpp maintainers).
 * Q4_0 is the format with the most optimised Arm CPU kernels in llama.cpp (KleidiAI and
 * runtime repacking), so it is the fastest 4-bit option on phones.
 *
 * The URL is pinned to an exact Hugging Face commit and the file is checked against its
 * SHA-256, so what runs on the phone is exactly what was tested.
 */
object ModelSpec {
    const val DISPLAY_NAME = "Qwen3-0.6B"
    const val QUANT = "Q4_0"
    const val FILE_NAME = "Qwen3-0.6B-Q4_0.gguf"
    const val SOURCE = "huggingface.co/ggml-org/Qwen3-0.6B-GGUF"
    const val LICENSE = "Apache-2.0"
    const val URL =
        "https://huggingface.co/ggml-org/Qwen3-0.6B-GGUF/resolve/" +
            "13ac06624948e4d8589cb4b6f90100837342fcb8/Qwen3-0.6B-Q4_0.gguf"
    const val SIZE_BYTES = 428_970_080L
    const val SHA256 = "da2572f16c06133561ce56accaa822216f2391ef4d37fba427801cd6736417d4"
}

/** A model file that is on the phone and ready to load. */
data class LocalModel(val file: File, val isBundledSpec: Boolean) {
    val label: String
        get() = if (isBundledSpec) "${ModelSpec.DISPLAY_NAME} · ${ModelSpec.QUANT}" else file.nameWithoutExtension
}

/**
 * Keeps model files in the app's private storage (removed when the app is uninstalled,
 * no storage permission needed) and handles download, resume, verification and import.
 */
class ModelStore(context: Context) {

    private val dir = File(context.filesDir, "models").apply { mkdirs() }

    private val specFile = File(dir, ModelSpec.FILE_NAME)
    private val specMarker = File(dir, ModelSpec.FILE_NAME + ".verified")
    private val partFile = File(dir, ModelSpec.FILE_NAME + ".part")

    private val customFile = File(dir, "custom.gguf")
    private val customMarker = File(dir, "custom.gguf.verified")

    /** The model to load, preferring the verified Qwen3 download over an imported file. */
    fun localModel(): LocalModel? = when {
        specFile.isFile && specMarker.isFile && specFile.length() == ModelSpec.SIZE_BYTES ->
            LocalModel(specFile, isBundledSpec = true)
        customFile.isFile && customMarker.isFile -> LocalModel(customFile, isBundledSpec = false)
        else -> null
    }

    /** Bytes already downloaded by an earlier, interrupted attempt. */
    fun partialBytes(): Long = if (partFile.isFile) partFile.length() else 0L

    fun freeBytes(): Long = dir.usableSpace

    /** Free space needed to finish the download, with a 64 MB safety margin. */
    fun bytesNeededToDownload(): Long =
        (ModelSpec.SIZE_BYTES - partialBytes()).coerceAtLeast(0L) + 64L * 1024 * 1024

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Downloads the model into a ".part" file, resuming where a previous attempt stopped
     * (HTTP Range). Retries transient network errors a few times before giving up.
     */
    suspend fun download(onProgress: (done: Long, total: Long) -> Unit) = withContext(Dispatchers.IO) {
        var attempt = 0
        while (true) {
            val before = partialBytes()
            try {
                downloadOnce(onProgress)
                return@withContext
            } catch (e: IOException) {
                // Only count attempts that made no progress, so a long download on a flaky
                // connection keeps going as long as bytes keep arriving.
                if (partialBytes() > before) attempt = 0
                attempt++
                if (attempt >= MAX_ATTEMPTS) throw e
                delay(2_000L * attempt)
            }
        }
    }

    private suspend fun downloadOnce(onProgress: (Long, Long) -> Unit) {
        val total = ModelSpec.SIZE_BYTES
        var existing = partialBytes()
        if (existing > total) {
            partFile.delete()
            existing = 0L
        }
        if (existing == total) {
            onProgress(total, total)
            return
        }

        val conn = (URL(ModelSpec.URL).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "HumanWonder-POC/0.1 (Android)")
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        try {
            val append = when (val code = conn.responseCode) {
                HttpURLConnection.HTTP_PARTIAL -> {
                    // Make sure the server really resumed where we asked it to.
                    val range = conn.getHeaderField("Content-Range") ?: ""
                    if (!range.startsWith("bytes $existing-")) {
                        partFile.delete()
                        throw IOException("Unexpected resume range '$range'; restarting download")
                    }
                    true
                }
                HttpURLConnection.HTTP_OK -> false // server ignored Range: start over
                416 -> {
                    partFile.delete()
                    throw IOException("Server rejected resume request; restarting download")
                }
                else -> throw IOException("Download failed: HTTP $code")
            }
            var done = if (append) existing else 0L
            onProgress(done, total)

            conn.inputStream.use { input ->
                FileOutputStream(partFile, append).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var lastReported = done
                    while (true) {
                        coroutineContext.ensureActive() // stop promptly when the user pauses
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (done - lastReported >= 1_048_576L) {
                            onProgress(done, total)
                            lastReported = done
                        }
                    }
                }
            }
            onProgress(done, total)
            if (done != total) throw IOException("Download incomplete: $done of $total bytes")
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Checks the downloaded file's SHA-256. On success it becomes the active model;
     * on failure the corrupt file is deleted so the next attempt starts clean.
     */
    suspend fun verifyDownload(onProgress: (Long, Long) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val hash = sha256(partFile, onProgress)
        if (hash == ModelSpec.SHA256) {
            specFile.delete()
            if (!partFile.renameTo(specFile)) throw IOException("Could not move the model into place")
            specMarker.writeText(hash)
            true
        } else {
            partFile.delete()
            false
        }
    }

    /**
     * Copies a .gguf file the user picked (e.g. sent over USB or Nearby Share) into private
     * storage. If it is byte-identical to the Qwen3 file it is used as such; any other
     * GGUF model is kept as a "custom" model.
     */
    suspend fun importFrom(
        resolver: ContentResolver,
        uri: Uri,
        onProgress: (Long, Long) -> Unit,
    ): LocalModel = withContext(Dispatchers.IO) {
        val total = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L
        if (total in 0 until 1_000_000L) throw IOException("That file is too small to be a language model.")
        if (total > 0 && total + 64L * 1024 * 1024 > freeBytes()) {
            throw IOException("Not enough free space to import this file.")
        }

        val tmp = File(dir, "import.tmp")
        val digest = MessageDigest.getInstance("SHA-256")
        var done = 0L
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { output ->
                val buffer = ByteArray(256 * 1024)
                var lastReported = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val n = input.read(buffer)
                    if (n < 0) break
                    if (done == 0L && !isGgufHeader(buffer, n)) {
                        throw IOException("That file is not a GGUF model.")
                    }
                    output.write(buffer, 0, n)
                    digest.update(buffer, 0, n)
                    done += n
                    if (done - lastReported >= 1_048_576L) {
                        onProgress(done, total)
                        lastReported = done
                    }
                }
            }
        } ?: throw IOException("Could not open that file.")
        onProgress(done, total)

        val hash = digest.digest().toHex()
        if (hash == ModelSpec.SHA256) {
            specFile.delete()
            tmp.renameTo(specFile)
            specMarker.writeText(hash)
            LocalModel(specFile, isBundledSpec = true)
        } else {
            customFile.delete()
            tmp.renameTo(customFile)
            customMarker.writeText(hash)
            LocalModel(customFile, isBundledSpec = false)
        }
    }

    private fun isGgufHeader(buffer: ByteArray, n: Int): Boolean =
        n >= 4 && buffer[0] == 'G'.code.toByte() && buffer[1] == 'G'.code.toByte() &&
            buffer[2] == 'U'.code.toByte() && buffer[3] == 'F'.code.toByte()

    private suspend fun sha256(file: File, onProgress: (Long, Long) -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val total = file.length()
        var done = 0L
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                coroutineContext.ensureActive()
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
                done += n
                onProgress(done, total)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
