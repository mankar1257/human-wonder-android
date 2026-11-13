package ai.humanwonder.poc.data.local

import ai.humanwonder.poc.data.local.dao.ConversationDao
import ai.humanwonder.poc.data.local.dao.MessageDao
import ai.humanwonder.poc.data.local.entities.Conversation
import ai.humanwonder.poc.data.local.entities.Message
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Conversation::class, Message::class],
    version = 2,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        /** Version 2: answers can carry a frame from Orbit's archive. Existing conversations are kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN frameId TEXT")
            }
        }
    }
}
