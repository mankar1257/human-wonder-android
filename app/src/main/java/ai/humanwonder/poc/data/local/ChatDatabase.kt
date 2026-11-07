package ai.humanwonder.poc.data.local

import ai.humanwonder.poc.data.local.dao.ConversationDao
import ai.humanwonder.poc.data.local.dao.MessageDao
import ai.humanwonder.poc.data.local.entities.Conversation
import ai.humanwonder.poc.data.local.entities.Message
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Conversation::class, Message::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
