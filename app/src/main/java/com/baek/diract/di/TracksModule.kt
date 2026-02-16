package com.baek.diract.di

import com.baek.diract.data.remote.api.TracksApi
import com.baek.diract.data.repository.TracksRepositoryImpl
import com.baek.diract.domain.repository.TracksRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TracksModule {

    companion object {
        @Provides
        @Singleton
        fun provideTracksApi(retrofit: Retrofit): TracksApi =
            retrofit.create(TracksApi::class.java)
    }

    @Binds
    @Singleton
    abstract fun bindTracksRepository(
        impl: TracksRepositoryImpl
    ): TracksRepository
}