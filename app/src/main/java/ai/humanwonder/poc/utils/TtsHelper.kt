package ai.humanwonder.poc.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import java.util.Locale
import java.util.UUID

/**
 * "Read answers aloud", ported from OfflineLLM's TtsHelper.
 * Human Wonder changes: Orbit 412's words in the notices, and a request made while the speech
 * engine is still starting is spoken as soon as it is ready instead of being dropped.
 */
class TtsHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var initFinished = false
    private var pending: Pair<String, () -> Unit>? = null
    private var onDoneCallback: (() -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.getDefault())
                isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                if (!isReady) {
                    val enResult = tts?.setLanguage(Locale.ENGLISH)
                    isReady = enResult != TextToSpeech.LANG_MISSING_DATA &&
                        enResult != TextToSpeech.LANG_NOT_SUPPORTED
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        handler.post { onDoneCallback?.invoke() }
                    }
                    @Deprecated("Deprecated")
                    override fun onError(utteranceId: String?) {
                        handler.post { onDoneCallback?.invoke() }
                    }
                })
            }
            handler.post {
                initFinished = true
                pending?.let { (text, done) ->
                    pending = null
                    speak(text, done)
                }
            }
        }
    }

    fun speak(text: String, onDone: () -> Unit = {}) {
        if (!initFinished) {
            pending = text to onDone
            return
        }
        onDoneCallback = onDone

        if (!isReady) {
            handler.post {
                Toast.makeText(
                    context,
                    "Orbit 412 can't speak on this phone yet. Turn on text-to-speech in your phone's settings.",
                    Toast.LENGTH_LONG
                ).show()
                onDone()
            }
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        if (result != TextToSpeech.SUCCESS) {
            handler.post {
                Toast.makeText(context, "Orbit 412 couldn't speak that answer.", Toast.LENGTH_SHORT).show()
                onDone()
            }
        }
    }

    fun stop() {
        pending = null
        tts?.stop()
        handler.post { onDoneCallback?.invoke() }
        onDoneCallback = null
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun shutdown() {
        handler.removeCallbacksAndMessages(null)
        pending = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
