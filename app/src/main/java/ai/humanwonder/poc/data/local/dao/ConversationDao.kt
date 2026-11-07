package ai.humanwonder.poc.data.local.dao

import ai.humanwonder.poc.data.local.entities.Conversation
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAllConversationsSync(): List<Conversation>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: String): Conversation?

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getMostRecent(): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: Conversation)

    @Update
    suspend fun update(conversation: Conversation)

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun rename(id: String, title: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getCount(): Int

    @Query("UPDATE conversations SET messageCount = messageCount + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementMessageCount(id: String, timestamp: Long = System.currentTimeMillis())

    /** Conversations whose title or any message contains [query] ("Search your transmissions"). */
    @Query(
        "SELECT * FROM conversations WHERE title LIKE '%' || :query || '%' " +
            "OR id IN (SELECT conversationId FROM messages WHERE content LIKE '%' || :query || '%') " +
            "ORDER BY updatedAt DESC"
    )
    fun search(query: String): Flow<List<Conversation>>
}
