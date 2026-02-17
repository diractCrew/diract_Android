package com.baek.diract.di

import com.baek.diract.data.remote.api.TrackApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackModule {
    //TODO: TrackRepository 따로 생성 시 주석 제거
//    @Binds
//    @Singleton
//    abstract fun bindTrackRepository(
//        impl: TrackRepositoryImpl
//    ): TrackRepository

    companion object {
        @Provides
        @Singleton
        fun provideTrackApi(retrofit: Retrofit): TrackApi {
            return retrofit.create(TrackApi::class.java)
        }
    }
}
