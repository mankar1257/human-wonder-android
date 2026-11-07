package ai.humanwonder.poc.ui.navigation

import ai.humanwonder.poc.ChatViewModel
import android.net.Uri

/**
 * Screens. First run: Landing → Name → Home. Later launches start at Home.
 * Home → Chat (new, an existing conversation, or a suggested question); back from Chat → Home.
 */
sealed class Routes(val route: String) {
    data object Landing : Routes("landing")
    data object Name : Routes("name")
    data object Home : Routes("home")
    data object Chat : Routes(
        "chat?${ChatViewModel.ARG_CONVERSATION}={${ChatViewModel.ARG_CONVERSATION}}" +
            "&${ChatViewModel.ARG_QUESTION}={${ChatViewModel.ARG_QUESTION}}",
    ) {
        /** A chat: new (no arguments), an existing conversation, or one that asks [question] at once. */
        fun to(conversationId: String? = null, question: String? = null): String {
            val args = buildList {
                if (conversationId != null) add("${ChatViewModel.ARG_CONVERSATION}=${Uri.encode(conversationId)}")
                if (question != null) add("${ChatViewModel.ARG_QUESTION}=${Uri.encode(question)}")
            }
            return if (args.isEmpty()) "chat" else "chat?" + args.joinToString("&")
        }
    }
    data object Profile : Routes("profile")
    data object Licenses : Routes("licenses")
}
