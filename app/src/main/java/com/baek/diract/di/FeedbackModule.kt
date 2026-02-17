package com.baek.diract.di

import com.baek.diract.data.remote.api.FeedbackApi
import com.baek.diract.data.repository.FeedbackRepositoryImpl
import com.baek.diract.domain.repository.FeedbackRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedbackModule {

    @Binds
    @Singleton
    abstract fun bindFeedbackRepository(
        impl: FeedbackRepositoryImpl
    ): FeedbackRepository

    companion object {
        @Provides
        @Singleton
        fun provideFeedbackApi(retrofit: Retrofit): FeedbackApi {
            return retrofit.create(FeedbackApi::class.java)
        }
    }
}
