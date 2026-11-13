package ai.humanwonder.poc.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // "user" | "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokenCount: Int = 0,
    /** The frame from Orbit's archive sent with an answer, if any (added in database version 2). */
    val frameId: String? = null,
)
