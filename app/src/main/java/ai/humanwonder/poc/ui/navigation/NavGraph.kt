package ai.humanwonder.poc.ui.navigation

import ai.humanwonder.poc.AppViewModel
import ai.humanwonder.poc.BuildConfig
import ai.humanwonder.poc.ChatViewModel
import ai.humanwonder.poc.ui.ChatScreen
import ai.humanwonder.poc.ui.LandingScreen
import ai.humanwonder.poc.ui.LicensesScreen
import ai.humanwonder.poc.ui.NameScreen
import ai.humanwonder.poc.ui.ProfileScreen
import ai.humanwonder.poc.ui.SideMenuDrawer
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

/** The Human Wonder screens (OfflineLLM's NavHost, with Human Wonder's routes and UI). */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    app: AppViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(500)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(500)) },
        popExitTransition = { fadeOut(tween(300)) },
    ) {
        composable(Routes.Landing.route) {
            val profile by app.profile.collectAsStateWithLifecycle()
            val returning = profile.onboardingComplete && profile.name.isNotBlank()
            LandingScreen(
                greetingName = if (returning) profile.name.trim() else null,
                onContinue = {
                    if (returning) {
                        // Profile already set up: straight into a new chat; Back from there comes home.
                        navController.navigate(Routes.Chat.to()) {
                            popUpTo(Routes.Landing.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(Routes.Name.route) { launchSingleTop = true }
                    }
                },
            )
        }

        composable(Routes.Name.route) {
            val profile by app.profile.collectAsStateWithLifecycle()
            NameScreen(
                initialName = profile.name,
                entering = false,
                onBack = { navController.popBackStack() },
                onEnter = { name ->
                    app.completeOnboarding(name)
                    navController.navigate(Routes.Chat.to()) {
                        popUpTo(Routes.Landing.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            Routes.Chat.route,
            arguments = listOf(
                navArgument(ChatViewModel.ARG_CONVERSATION) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(ChatViewModel.ARG_QUESTION) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { ChatRoute(app, navController) }

        composable(Routes.Profile.route) { ProfileRoute(app, navController) }

        composable(Routes.Licenses.route) { LicensesScreen(onBack = { navController.popBackStack() }) }
    }
}

/** The side menu wired to the app state; [selectedId] is the conversation open in the chat, if any. */
@Composable
private fun SideMenu(
    app: AppViewModel,
    drawerState: DrawerState,
    selectedId: String?,
    onHome: () -> Unit,
    onNewConversation: () -> Unit,
    onOpen: (String) -> Unit,
    onDeleted: (String) -> Unit,
    onProfile: () -> Unit,
    content: @Composable () -> Unit,
) {
    val profile by app.profile.collectAsStateWithLifecycle()
    val conversations by app.drawerConversations.collectAsStateWithLifecycle()
    val query by app.searchQuery.collectAsStateWithLifecycle()
    SideMenuDrawer(
        drawerState = drawerState,
        name = profile.name,
        conversations = conversations,
        query = query,
        onQuery = app::setSearchQuery,
        selectedId = selectedId,
        onHome = onHome,
        onNewConversation = onNewConversation,
        onOpen = onOpen,
        onRename = app::renameConversation,
        onDelete = { id ->
            app.deleteConversation(id)
            onDeleted(id)
        },
        onProfile = onProfile,
        content = content,
    )
}

@Composable
private fun ChatRoute(app: AppViewModel, nav: NavHostController) {
    val vm: ChatViewModel = hiltViewModel()
    val state by vm.ui.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Voice input through the phone's own speech recogniser (the mic in the composer).
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) input = if (input.isBlank()) spoken else "$input $spoken"
    }
    val startVoice: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to the sky")
        try {
            voice.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Voice input isn't available on this phone.", Toast.LENGTH_SHORT).show()
        }
    }

    // Keep the screen awake while Orbit 412 is answering.
    val view = LocalView.current
    DisposableEffect(state.generating) {
        view.keepScreenOn = state.generating
        onDispose { view.keepScreenOn = false }
    }

    // Home is the desert sky: it becomes the only screen again, so Back there leaves the app.
    val goHome: () -> Unit = {
        nav.navigate(Routes.Landing.route) {
            popUpTo(nav.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
    // Back from the chat goes home to the sky (the side menu, when open, closes first).
    BackHandler(enabled = drawerState.isClosed) { goHome() }

    SideMenu(
        app = app,
        drawerState = drawerState,
        selectedId = state.conversationId,
        onHome = goHome,
        onNewConversation = {
            vm.newConversation()
            input = ""
        },
        onOpen = { id -> vm.openConversation(id) },
        onDeleted = { id -> if (id == state.conversationId) vm.newConversation() },
        onProfile = { nav.navigate(Routes.Profile.route) { launchSingleTop = true } },
    ) {
        ChatScreen(
            state = state,
            input = input,
            onInput = { input = it },
            onSend = vm::send,
            onStop = vm::stop,
            onNewConversation = vm::newConversation,
            onMenu = { scope.launch { drawerState.open() } },
            onPerspective = vm::setPerspective,
            onCycleTone = vm::cycleTone,
            onMic = startVoice,
            onRetry = app::retry,
        )
    }
}

@Composable
private fun ProfileRoute(app: AppViewModel, nav: NavHostController) {
    val profile by app.profile.collectAsStateWithLifecycle()
    // Export through the system file picker (no storage permission needed).
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) app.exportTo(uri)
    }
    ProfileScreen(
        profile = profile,
        version = BuildConfig.VERSION_NAME,
        isValidEmail = { AppViewModel.isValidEmail(it) },
        onBack = { nav.popBackStack() },
        onName = app::setName,
        onEmail = app::setEmail,
        onCity = app::setCity,
        onPerspective = app::setPerspective,
        onTone = app::setTone,
        onReadAloud = app::setReadAloud,
        onHaptics = app::setHaptics,
        onExport = { export.launch("human-wonder-transmissions.json") },
        onClearAll = app::clearAllConversations,
        onLicenses = { nav.navigate(Routes.Licenses.route) { launchSingleTop = true } },
    )
}
