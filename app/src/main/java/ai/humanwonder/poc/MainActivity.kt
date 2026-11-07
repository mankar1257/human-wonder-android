package ai.humanwonder.poc

import ai.humanwonder.poc.ui.ChatScreen
import ai.humanwonder.poc.ui.GalaxyLifecycle
import ai.humanwonder.poc.ui.HW
import ai.humanwonder.poc.ui.HumanWonderTheme
import ai.humanwonder.poc.ui.LandingScreen
import ai.humanwonder.poc.ui.NameScreen
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    private val vm: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always light status/nav icons: the sky is dark even when the phone is in light mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        setContent {
            HumanWonderTheme {
                val state by vm.ui.collectAsState()
                var input by rememberSaveable { mutableStateOf("") }

                // Voice input through the phone's own speech recogniser (the mic in the Stitch prompt box).
                val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                    if (!spoken.isNullOrBlank()) input = if (input.isBlank()) spoken else "$input $spoken"
                }
                val startVoice = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to the sky")
                    try {
                        voice.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(this, "Voice input isn't available on this phone.", Toast.LENGTH_SHORT).show()
                    }
                }

                // Keep the screen awake while the sky is waking up or answering.
                val busy = state.generating || state.sky !is SkyState.Ready && state.sky !is SkyState.Failed
                LaunchedEffect(busy) {
                    if (busy) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                BackHandler(enabled = state.screen == Screen.Name) { vm.backFromName() }

                Box(Modifier.fillMaxSize().background(HW.Space)) {
                    Crossfade(targetState = state.screen, animationSpec = tween(700), label = "screen") { screen ->
                        when (screen) {
                            Screen.Landing -> LandingScreen(onBegin = vm::begin)
                            Screen.Name -> NameScreen(
                                initialName = state.name,
                                entering = state.entering,
                                onBack = { vm.backFromName() },
                                onEnter = vm::enter,
                            )
                            Screen.Chat -> ChatScreen(
                                state = state,
                                input = input,
                                onInput = { input = it },
                                onSend = vm::send,
                                onStop = vm::stop,
                                onNewConversation = vm::newConversation,
                                onBack = vm::editName,
                                onPerspective = vm::setPerspective,
                                onCycleTone = vm::cycleTone,
                                onMic = startVoice,
                                onRetry = vm::retry,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        GalaxyLifecycle.onPause()
    }

    override fun onResume() {
        super.onResume()
        GalaxyLifecycle.onResume()
    }
}
