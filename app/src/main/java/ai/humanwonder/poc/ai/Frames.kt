package ai.humanwonder.poc.ai

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Orbit's archive: astronaut photographs of Earth (and short loops cut from the station's
 * time-lapses) that ship inside the app under assets/frames/. One can be sent with an answer.
 *
 * The catalog (frames.json) is shared with the iPhone app and with tools/frames/pick.py, which is
 * the reference for [FramePicker]; tools/frames/pick-cases.json checks all three agree.
 */
@Serializable
data class Frame(
    val id: String,
    /** "photo" (a JPEG) or "loop" (an animated WebP). */
    val kind: String,
    /** "night", "day" or "any". */
    val time: String,
    /** The NASA frame number(s), for the credit. */
    val source: String,
    /** Short title under the picture. */
    val caption: String,
    /** What the picture shows, in words for the model ("the Gulf at night, with …"). */
    val says: String,
    /** Words in a question that point to this picture, with how strongly (1 to 3). */
    val keys: Map<String, Int>,
) {
    val isLoop: Boolean get() = kind == "loop"

    /** Path of the picture inside the app's assets. */
    val assetPath: String get() = "frames/$id." + if (isLoop) "webp" else "jpg"
}

@Serializable
data class FrameRegion(val id: String, val keys: List<String>, val night: List<String>, val day: List<String>)

@Serializable
data class FrameCatalog(
    val version: Int = 1,
    val credit: String,
    @kotlinx.serialization.SerialName("show_words") val showWords: List<String>,
    @kotlinx.serialization.SerialName("see_words") val seeWords: List<String>,
    @kotlinx.serialization.SerialName("about_me_words") val aboutMeWords: List<String>,
    @kotlinx.serialization.SerialName("loop_words") val loopWords: List<String>,
    @kotlinx.serialization.SerialName("night_words") val nightWords: List<String>,
    @kotlinx.serialization.SerialName("day_words") val dayWords: List<String>,
    val regions: List<FrameRegion>,
    val frames: List<Frame>,
) {
    private val byId: Map<String, Frame> by lazy { frames.associateBy { it.id } }

    fun frame(id: String?): Frame? = id?.let { byId[it] }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): FrameCatalog = json.decodeFromString(serializer(), text)

        @Volatile private var cached: FrameCatalog? = null

        /** The bundled catalog (read once). Empty if the assets are missing, so chat still works. */
        fun load(context: Context): FrameCatalog = cached ?: synchronized(this) {
            cached ?: runCatching {
                parse(context.assets.open("frames/frames.json").bufferedReader().use { it.readText() })
            }.getOrElse { EMPTY }.also { cached = it }
        }

        val EMPTY = FrameCatalog(
            credit = "", showWords = emptyList(), seeWords = emptyList(), aboutMeWords = emptyList(),
            loopWords = emptyList(), nightWords = emptyList(), dayWords = emptyList(),
            regions = emptyList(), frames = emptyList(),
        )
    }
}

/**
 * Chooses the picture for an answer from the question alone, before the model starts, so the
 * model can be told what it is sending. The app chooses rather than the model: tested on the
 * app's own model, letting it choose found the right picture for fewer than 1 in 7 questions
 * (tools/frames/README.md). Same steps as tools/frames/pick.py.
 */
object FramePicker {
    private const val MIN_SCORE = 3

    /** Lower case; anything that is not a letter, digit or combining mark becomes one space. */
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length + 2).append(' ')
        var space = true
        var i = 0
        val lower = text.lowercase()
        while (i < lower.length) {
            val cp = lower.codePointAt(i)
            val keep = Character.isLetterOrDigit(cp) || when (Character.getType(cp)) {
                Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt(),
                Character.ENCLOSING_MARK.toInt(), Character.LETTER_NUMBER.toInt(), Character.OTHER_NUMBER.toInt() -> true
                else -> false
            }
            if (keep) {
                sb.appendCodePoint(cp)
                space = false
            } else if (!space) {
                sb.append(' ')
                space = true
            }
            i += Character.charCount(cp)
        }
        if (!space) sb.append(' ')
        return sb.toString()
    }

    private fun isAscii(s: String) = s.all { it.code < 128 }

    fun has(text: String, key: String): Boolean {
        val k = normalize(key).trim()
        if (k.isEmpty()) return false
        if (text.contains(" $k ")) return true
        return isAscii(k) && text.contains(" ${k}s ")
    }

    private fun score(text: String, frame: Frame): Int = frame.keys.entries.sumOf { (k, w) -> if (has(text, k)) w else 0 }

    /**
     * The picture to send with the answer to [question], or null.
     * [context]: the previous question and answer, for "can you show me?" follow-ups.
     * [shown]: ids already sent in this conversation (never sent twice).
     * [hour]: the person's local hour, used when the question says neither day nor night.
     * [city]: where they usually look up from, for "what do you see?".
     */
    fun pick(
        catalog: FrameCatalog,
        question: String,
        context: String = "",
        shown: Set<String> = emptySet(),
        hour: Int? = null,
        city: String = "",
    ): Frame? {
        if (catalog.frames.isEmpty()) return null
        val q = normalize(question)
        val asks = question.contains('?') || question.contains('؟')
        val wantsShow = catalog.showWords.any { has(q, it) } || (asks && catalog.seeWords.any { has(q, it) })
        val wantsLoop = catalog.loopWords.any { has(q, it) }
        val saysNight = catalog.nightWords.any { has(q, it) }
        val saysDay = catalog.dayWords.any { has(q, it) }
        val clockNight = hour != null && (hour >= 19 || hour < 6)
        val night = saysNight || (!saysDay && clockNight)

        val frames = catalog.frames
        var scores = frames.map { score(q, it) }
        if ((scores.maxOrNull() ?: 0) < MIN_SCORE && wantsShow && context.isNotBlank()) {
            val c = normalize(context)
            scores = scores.mapIndexed { i, s -> s + score(c, frames[i]) }
        }

        var best: Frame? = null
        var bestRank = Int.MIN_VALUE
        frames.forEachIndexed { i, f ->
            val s = scores[i]
            if (s < MIN_SCORE || f.id in shown) return@forEachIndexed
            var rank = s
            if (wantsLoop && f.isLoop) rank += 2
            if (f.time != "any") {
                if (night) {
                    rank += if (f.time == "night") 2 else -2
                } else if (saysDay || hour != null) {
                    rank += if (f.time == "day") 2 else -2
                }
            }
            if (rank > bestRank) {
                best = f
                bestRank = rank
            }
        }
        if (best != null) return best
        if (!wantsShow || catalog.aboutMeWords.any { has(q, it) }) return null

        // "What do you see?" with no subject: a view over the person's part of the world.
        val where = q + " " + normalize(context) + " " + normalize(city)
        val region = catalog.regions.firstOrNull { r -> r.keys.any { has(where, it) } } ?: catalog.regions.lastOrNull() ?: return null
        val choices = (if (night) region.night else region.day).mapNotNull { catalog.frame(it) }
        val ordered = choices.sortedBy { if (wantsLoop && it.isLoop) 0 else 1 }
        return ordered.firstOrNull { it.id !in shown }
    }
}
