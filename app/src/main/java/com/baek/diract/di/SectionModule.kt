package com.baek.diract.di

import com.baek.diract.data.remote.api.SectionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SectionModule {

    companion object {
        @Provides
        @Singleton
        fun provideSectionApi(retrofit: Retrofit): SectionApi {
            return retrofit.create(SectionApi::class.java)
        }
    }
}
