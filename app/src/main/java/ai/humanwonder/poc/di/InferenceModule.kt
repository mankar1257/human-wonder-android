package ai.humanwonder.poc.di

import ai.humanwonder.poc.ai.InferenceEngine
import ai.humanwonder.poc.ai.ModelManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InferenceModule {

    @Provides
    @Singleton
    fun provideInferenceEngine(): InferenceEngine = InferenceEngine()

    @Provides
    @Singleton
    fun provideModelManager(
        @ApplicationContext context: Context,
        inferenceEngine: InferenceEngine,
    ): ModelManager = ModelManager(context, inferenceEngine)
}
