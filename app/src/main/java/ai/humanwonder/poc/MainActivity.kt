package ai.humanwonder.poc

import ai.humanwonder.poc.ui.ChatScreen
import ai.humanwonder.poc.ui.ConsentScreen
import ai.humanwonder.poc.ui.FailedScreen
import ai.humanwonder.poc.ui.HumanWonderTheme
import ai.humanwonder.poc.ui.LoadingScreen
import ai.humanwonder.poc.ui.ProgressScreen
import ai.humanwonder.poc.ui.SkyBackground
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private val vm: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HumanWonderTheme {
                val state by vm.ui.collectAsState()
                val pickModel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) vm.importModel(uri)
                }
                val importModel = { pickModel.launch(arrayOf("*/*")) }

                // Keep the screen awake while downloading or answering so Android doesn't
                // throttle the work in the middle.
                val busy = state.generating || state.phase is Phase.Downloading ||
                    state.phase is Phase.Verifying || state.phase is Phase.Importing ||
                    state.phase is Phase.LoadingModel
                LaunchedEffect(busy) {
                    if (busy) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                SkyBackground {
                    when (val phase = state.phase) {
                        Phase.Starting, Phase.LoadingModel -> LoadingScreen(state.modelLabel)

                        is Phase.NeedsConsent -> ConsentScreen(
                            check = phase.check,
                            onAllow = vm::startDownload,
                            onImport = importModel,
                        )

                        is Phase.Downloading -> ProgressScreen(
                            title = "Bringing the sky's voice down…",
                            subtitle = "A one-time download. Keep the app open; if you pause or lose " +
                                "signal, it picks up where it stopped.",
                            done = phase.done,
                            total = phase.total,
                            actionLabel = "Pause",
                            onAction = vm::pauseDownload,
                        )

                        is Phase.Verifying -> ProgressScreen(
                            title = "Checking the file…",
                            subtitle = "Making sure every byte arrived exactly as published.",
                            done = phase.done,
                            total = phase.total,
                        )

                        is Phase.Importing -> ProgressScreen(
                            title = "Importing the model…",
                            subtitle = "Copying the file into the app's private storage.",
                            done = phase.done,
                            total = phase.total,
                        )

                        is Phase.Failed -> FailedScreen(
                            message = phase.message,
                            offerRedownload = phase.offerRedownload,
                            onRetry = vm::retry,
                            onDeleteAndRedownload = vm::deleteModelAndRestart,
                            onImport = importModel,
                        )

                        Phase.Ready -> ChatScreen(
                            state = state,
                            onSend = vm::send,
                            onStop = vm::stop,
                            onNewChat = vm::newChat,
                            onDeleteModel = vm::deleteModelAndRestart,
                        )
                    }
                }
            }
        }
    }
}
