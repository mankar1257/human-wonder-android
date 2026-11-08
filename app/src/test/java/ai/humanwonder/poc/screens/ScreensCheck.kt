package ai.humanwonder.poc.screens

import ai.humanwonder.poc.ChatMessage
import ai.humanwonder.poc.SkyState
import ai.humanwonder.poc.UiState
import ai.humanwonder.poc.data.local.entities.Conversation
import ai.humanwonder.poc.data.repository.Profile
import ai.humanwonder.poc.ui.ChatScreen
import ai.humanwonder.poc.ui.HumanWonderScale
import ai.humanwonder.poc.ui.HumanWonderTheme
import ai.humanwonder.poc.ui.LandingScreen
import ai.humanwonder.poc.ui.LocalLiveSky
import ai.humanwonder.poc.ui.NameScreen
import ai.humanwonder.poc.ui.ProfileScreen
import ai.humanwonder.poc.ui.SideMenuDrawer
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screen checks: draws every screen of the app on several kinds of phone and saves a PNG of each
 * into app/build/screens-check/<phone>/ (tools/build-on-mac.sh copies them to screens-check/).
 * Nothing is compared automatically; the pictures are for people to look at.
 *
 * Each subclass at the bottom is one phone set-up. The screens are wrapped in [HumanWonderScale]
 * exactly as MainActivity does, so the Font size and Display size limits are part of the check.
 * The phone's status bar, navigation bar and keyboard are stood in for by plain bands, because the
 * test window has none.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
abstract class ScreensCheck(private val phone: String) {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun landingFirstTime() = shoot("01-landing-first-time") { LandingScreen(greetingName = null, onContinue = {}) }

    @Test fun landingWelcomeBack() = shoot("02-landing-welcome-back") { LandingScreen(greetingName = "Vivek", onContinue = {}) }

    @Test fun name() = shoot("03-name") { NameScreen(initialName = "", entering = false, onBack = {}, onEnter = {}) }

    @Test fun nameWithKeyboard() = shoot("04-name-keyboard", keyboard = true) {
        NameScreen(initialName = "Vivek", entering = false, onBack = {}, onEnter = {})
    }

    @Test fun chatEmpty() = shoot("05-chat-empty") { Chat(chatState()) }

    @Test fun chatEmptyWithKeyboard() = shoot("06-chat-empty-keyboard", keyboard = true) { Chat(chatState()) }

    @Test fun chatConversation() = shoot("07-chat-conversation") { Chat(chatState(CONVERSATION)) }

    @Test fun chatConversationWithKeyboard() = shoot("08-chat-conversation-keyboard", keyboard = true) {
        Chat(chatState(CONVERSATION))
    }

    @Test fun sideMenu() = shoot("09-side-menu") {
        SideMenuDrawer(
            drawerState = rememberDrawerState(DrawerValue.Open),
            name = "Vivek",
            conversations = CONVERSATIONS,
            query = "",
            onQuery = {},
            selectedId = CONVERSATIONS.first().id,
            onHome = {},
            onNewConversation = {},
            onOpen = {},
            onRename = { _, _ -> },
            onDelete = {},
            onProfile = {},
        ) { Chat(chatState(CONVERSATION)) }
    }

    @Test fun profile() = shoot("10-profile") {
        ProfileScreen(
            profile = Profile(name = "Vivek", city = "Abu Dhabi", onboardingComplete = true),
            version = "2.0",
            isValidEmail = { true },
            onBack = {},
            onName = {},
            onEmail = {},
            onCity = {},
            onPerspective = {},
            onTone = {},
            onReadAloud = {},
            onHaptics = {},
            onExport = {},
            onClearAll = {},
            onLicenses = {},
        )
    }

    /** Draws [screen] inside a stand-in phone frame, lets its opening animations finish, saves it. */
    private fun shoot(name: String, keyboard: Boolean = false, screen: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false // the sky's animations never "finish"
        compose.setContent {
            CompositionLocalProvider(LocalLiveSky provides false) {
                Column(Modifier.fillMaxSize().background(Color.Black)) {
                    Spacer(Modifier.fillMaxWidth().height(STATUS_BAR)) // status bar
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        HumanWonderScale { HumanWonderTheme { screen() } }
                    }
                    if (keyboard) KeyboardStandIn() else Spacer(Modifier.fillMaxWidth().height(NAV_BAR))
                }
            }
        }
        compose.mainClock.advanceTimeBy(2_400)
        val bitmap = runCatching { compose.onRoot().captureToImage().asAndroidBitmap() }
            .getOrElse { drawWindow() }
        val file = File(OUT_DIR, "$phone/$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Fallback capture: draw the whole window into a bitmap. */
    private fun drawWindow(): Bitmap {
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    @Composable
    private fun Chat(state: UiState) = ChatScreen(
        state = state,
        input = "",
        onInput = {},
        onSend = {},
        onStop = {},
        onNewConversation = {},
        onMenu = {},
        onPerspective = {},
        onCycleTone = {},
        onMic = {},
        onRetry = {},
    )

    /** A grey keyboard-sized band with key shapes, standing in for the phone's keyboard. */
    @Composable
    private fun KeyboardStandIn() {
        Column(
            Modifier.fillMaxWidth().height(KEYBOARD).background(Color(0xFF1E1F22)).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(30.dp)) // suggestion strip
            listOf(10, 9, 9, 5).forEach { keys ->
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(keys) {
                        Box(Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(6.dp)).background(Color(0xFF3A3B3F)))
                    }
                }
            }
        }
    }

    private companion object {
        val OUT_DIR = File(System.getProperty("hw.screens.dir") ?: "build/screens-check")
        val STATUS_BAR = 32.dp
        val NAV_BAR = 20.dp
        val KEYBOARD = 300.dp

        fun chatState(messages: List<ChatMessage> = emptyList()) = UiState(
            name = "Vivek",
            sky = SkyState.Ready,
            conversationId = if (messages.isEmpty()) null else "c1",
            messages = messages,
        )

        val CONVERSATION = listOf(
            ChatMessage("m1", fromUser = true, text = "What are you seeing over Liwa dunes right now?"),
            ChatMessage(
                "m2", fromUser = false,
                text = "The dunes are a quiet sea of shadow tonight, Vivek. Their crests catch a thin " +
                    "silver line of moonlight, and far to the north the lights of Abu Dhabi glow like " +
                    "embers at the edge of the dark.",
            ),
            ChatMessage("m3", fromUser = true, text = "Can you spot our campfire in the desert?"),
            ChatMessage(
                "m4", fromUser = false,
                text = "A single warm point, south of the last road. From 412 km up it is smaller than " +
                    "a star, but I see it. Keep it burning and I will look for it on my next pass.",
            ),
        )

        val CONVERSATIONS = listOf(
            Conversation(id = "c1", title = "Liwa dunes tonight", messageCount = 4),
            Conversation(id = "c2", title = "City lights from 400 km", messageCount = 6),
            Conversation(id = "c3", title = "Our campfire in the desert", messageCount = 2),
        )
    }
}

/** A typical phone today (Pixel 8 class, 393 x 852 dp) at the standard text size: the preview. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi")
class TypicalPhone : ScreensCheck("1-typical-phone")

/** The same phone with Font size turned up (1.3x): text may grow 15%, no more. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi", fontScale = 1.3f)
class LargerText : ScreensCheck("2-larger-text")

/** Font size at its largest (2x): must look the same as 2-larger-text. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi", fontScale = 2.0f)
class LargestText : ScreensCheck("3-largest-text")

/** Display size / Screen zoom at its largest (the phone reports only 320 dp) plus larger text. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h693dp-xxhdpi", fontScale = 1.3f)
class ZoomedDisplay : ScreensCheck("4-zoomed-display")

/** A small, short phone (360 x 640 dp) with slightly larger text. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.15f)
class SmallPhone : ScreensCheck("5-small-phone")
