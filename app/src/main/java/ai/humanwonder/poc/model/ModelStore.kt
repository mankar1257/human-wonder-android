package ai.humanwonder.poc.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * The language model ships inside the APK (assets/hw-core.gguf, stored uncompressed).
 * llama.cpp needs a real file path to memory-map, so on first launch the asset is copied
 * once into the app's private storage. Later launches reuse that copy.
 */
class ModelStore(private val context: Context) {

    private val dir = File(context.filesDir, "models").apply { mkdirs() }
    private val modelFile = File(dir, ASSET_NAME)
    private val marker = File(dir, "$ASSET_NAME.ready")

    /** Returns the model file, copying it out of the APK first if needed. */
    suspend fun prepare(onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val (total, open) = openAsset()
        if (modelFile.length() == total && marker.isFile && marker.readText().trim() == total.toString()) {
            onProgress(1f)
            return@withContext modelFile
        }

        // Clear out anything older (e.g. a model downloaded by an earlier POC build).
        dir.listFiles()?.forEach { if (it.name != ASSET_NAME) it.delete() }
        if (total > 0 && dir.usableSpace < total - modelFile.length() + 64L * 1024 * 1024) {
            throw IOException(
                "Not enough free space on this phone: about ${total / 1_000_000 + 64} MB is needed.",
            )
        }

        val tmp = File(dir, "$ASSET_NAME.tmp")
        open().use { input ->
            FileOutputStream(tmp).use { output ->
                val buffer = ByteArray(1 shl 20)
                var done = 0L
                var lastReported = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val n = input.read(buffer)
                    if (n < 0) break
                    output.write(buffer, 0, n)
                    done += n
                    if (total > 0 && done - lastReported >= 4L * 1024 * 1024) {
                        onProgress(done.toFloat() / total)
                        lastReported = done
                    }
                }
                output.fd.sync()
            }
        }
        if (total > 0 && tmp.length() != total) throw IOException("Copy of the model was incomplete.")
        modelFile.delete()
        if (!tmp.renameTo(modelFile)) throw IOException("Could not move the model into place.")
        marker.writeText(modelFile.length().toString())
        onProgress(1f)
        modelFile
    }

    /** Size of the bundled asset (or -1 if unknown) and a way to open it. */
    private fun openAsset(): Pair<Long, () -> java.io.InputStream> = try {
        val afd = context.assets.openFd(ASSET_NAME) // works because the asset is stored uncompressed
        val length = afd.length
        afd.close()
        length to { context.assets.openFd(ASSET_NAME).createInputStream() }
    } catch (e: IOException) {
        // Fallback if the asset was compressed by a different build setup.
        -1L to { context.assets.open(ASSET_NAME) }
    }

    private companion object {
        const val ASSET_NAME = "hw-core.gguf"
    }
}
