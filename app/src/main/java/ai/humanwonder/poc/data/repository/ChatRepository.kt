package ai.humanwonder.poc.data.repository

import ai.humanwonder.poc.data.local.dao.ConversationDao
import ai.humanwonder.poc.data.local.dao.MessageDao
import ai.humanwonder.poc.data.local.entities.Conversation
import ai.humanwonder.poc.data.local.entities.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ExportedChat(
    val title: String,
    val createdAt: Long,
    val messages: List<ExportedMessage>
)

@Serializable
data class ExportedMessage(
    val role: String,
    val content: String,
    val timestamp: Long,
    /** The frame from Orbit's archive sent with an answer, if any. */
    val frame: String? = null,
)

@Serializable
data class ExportData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val chats: List<ExportedChat>
)

/** Conversations and messages (Room), ported from OfflineLLM; the model registry is gone. */
@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
) {
    // Building a Json instance is expensive enough that kotlinx warns about doing it per call.
    private val exportJson = Json { prettyPrint = true }

    // Conversations
    fun getAllConversations(): Flow<List<Conversation>> = conversationDao.getAllConversations()

    suspend fun getConversation(id: String): Conversation? = conversationDao.getConversation(id)

    suspend fun getMostRecentConversation(): Conversation? = conversationDao.getMostRecent()

    suspend fun createConversation(title: String): Conversation {
        val conversation = Conversation(title = title)
        conversationDao.insert(conversation)
        return conversation
    }

    suspend fun renameConversation(id: String, title: String) = conversationDao.rename(id, title)

    suspend fun deleteConversation(id: String) {
        messageDao.deleteAllForConversation(id)
        conversationDao.delete(id)
    }

    suspend fun deleteAllConversations() {
        messageDao.deleteAll()
        conversationDao.deleteAll()
    }

    suspend fun getConversationCount(): Int = conversationDao.getCount()

    fun searchConversations(query: String): Flow<List<Conversation>> = conversationDao.search(query)

    // Messages
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesForConversation(conversationId)

    suspend fun getMessagesSync(conversationId: String): List<Message> =
        messageDao.getMessagesForConversationSync(conversationId)

    suspend fun addMessage(
        conversationId: String,
        role: String,
        content: String,
        id: String = UUID.randomUUID().toString(),
        frameId: String? = null,
    ): Message {
        val message = Message(
            id = id,
            conversationId = conversationId,
            role = role,
            content = content,
            frameId = frameId,
        )
        messageDao.insert(message)
        conversationDao.incrementMessageCount(conversationId)
        return message
    }

    suspend fun deleteMessage(id: String) = messageDao.delete(id)

    /** Every conversation as pretty-printed JSON (OfflineLLM's export format, without its unused fields). */
    suspend fun exportAllToJson(): String {
        val exportedChats = conversationDao.getAllConversationsSync().map { conv ->
            ExportedChat(
                title = conv.title,
                createdAt = conv.createdAt,
                messages = messageDao.getMessagesForConversationSync(conv.id).map { msg ->
                    ExportedMessage(role = msg.role, content = msg.content, timestamp = msg.timestamp, frame = msg.frameId)
                },
            )
        }
        return exportJson.encodeToString(ExportData(chats = exportedChats))
    }
}
