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
    //TODO: SectionRepository 따로 생성 시 주석 제거
//    @Binds
//    @Singleton
//    abstract fun bindSectionRepository(
//        impl: SectionRepositoryImpl
//    ): SectionRepository

    companion object {
        @Provides
        @Singleton
        fun provideSectionApi(retrofit: Retrofit): SectionApi {
            return retrofit.create(SectionApi::class.java)
        }
    }
}
