package ai.humanwonder.poc.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** One conversation with Orbit 412 (a "transmission" in the side menu). From OfflineLLM, trimmed. */
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "New conversation",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
)
