package ai.humanwonder.poc.utils

import android.content.Context
import android.net.Uri

/** From OfflineLLM's FileUtils (only what Human Wonder uses), plus bundled-text reading. */
object FileUtils {

    fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(text.toByteArray())
            } != null
        } catch (_: Exception) {
            false
        }
    }

    /** Text files bundled under assets/[dir], by file name, or empty if the folder is missing. */
    fun readAssetTexts(context: Context, dir: String): List<Pair<String, String>> = try {
        context.assets.list(dir).orEmpty().sorted().map { name ->
            name to context.assets.open("$dir/$name").bufferedReader().use { it.readText() }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
