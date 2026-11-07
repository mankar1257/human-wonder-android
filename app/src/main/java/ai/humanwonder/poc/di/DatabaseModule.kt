package ai.humanwonder.poc.di

import ai.humanwonder.poc.data.local.ChatDatabase
import ai.humanwonder.poc.data.local.dao.ConversationDao
import ai.humanwonder.poc.data.local.dao.MessageDao
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase {
        return Room.databaseBuilder(
            context,
            ChatDatabase::class.java,
            "humanwonder_database"
        ).build()
    }

    @Provides
    fun provideConversationDao(db: ChatDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideMessageDao(db: ChatDatabase): MessageDao = db.messageDao()
}
