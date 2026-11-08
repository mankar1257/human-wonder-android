package ai.humanwonder.poc.ui.navigation

import ai.humanwonder.poc.ChatViewModel
import android.net.Uri

/**
 * Screens. Every launch opens on Landing (the desert sky). Swipe up: first time → Name → Chat;
 * profile already set up → Chat. In the chat, the side menu's Home (and Back) return to Landing.
 */
sealed class Routes(val route: String) {
    data object Landing : Routes("landing")
    data object Name : Routes("name")
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
