package ai.humanwonder.poc

import ai.humanwonder.poc.ui.GalaxyLifecycle
import ai.humanwonder.poc.ui.HW
import ai.humanwonder.poc.ui.HumanWonderTheme
import ai.humanwonder.poc.ui.navigation.NavGraph
import ai.humanwonder.poc.ui.navigation.Routes
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.min

/**
 * The single activity (OfflineLLM's pattern: Hilt entry point + Navigation Compose). Tamper
 * checks and the biometric lock are gone; the UI is Human Wonder's.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val app: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always light status/nav icons: the sky is dark even when the phone is in light mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        // Every launch opens on the desert sky; swiping up goes to Name (first time) or the chat.
        val start = Routes.Landing.route

        setContent {
            // Human Wonder is laid out for the phone's normal text size. A larger system "Font size"
            // still makes text bigger, but at most 15%, so screens keep their designed proportions
            // instead of words crowding everything else off the screen.
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, min(base.fontScale, MAX_FONT_SCALE)),
            ) {
            HumanWonderTheme {
                val sky by app.sky.collectAsStateWithLifecycle()

                // Keep the screen awake while the sky is waking up (first launch unpacks it).
                val waking = sky is SkyState.Aligning || sky == SkyState.Waking
                LaunchedEffect(waking) {
                    if (waking) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                Box(Modifier.fillMaxSize().background(HW.Space)) {
                    NavGraph(
                        navController = rememberNavController(),
                        startDestination = start,
                        app = app,
                    )
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

/** The largest text scale the app follows from the phone's Font size setting. */
private const val MAX_FONT_SCALE = 1.15f
