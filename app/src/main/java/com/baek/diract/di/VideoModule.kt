package com.baek.diract.di

import android.content.ContentResolver
import android.content.Context
import com.baek.diract.data.remote.api.VideoApi
import com.baek.diract.data.repository.GalleryRepositoryImpl
import com.baek.diract.data.repository.VideoRepositoryImpl
import com.baek.diract.domain.repository.GalleryRepository
import com.baek.diract.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VideoModule {

    @Binds
    @Singleton
    abstract fun bindVideoRepository(
        impl: VideoRepositoryImpl
    ): VideoRepository

    @Binds
    @Singleton
    abstract fun bindGalleryRepository(
        impl: GalleryRepositoryImpl
    ): GalleryRepository

    companion object {
        @Provides
        @Singleton
        fun provideVideoApi(retrofit: Retrofit): VideoApi {
            return retrofit.create(VideoApi::class.java)
        }

        @Provides
        @Singleton
        fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
            return context.contentResolver
        }
    }
}
