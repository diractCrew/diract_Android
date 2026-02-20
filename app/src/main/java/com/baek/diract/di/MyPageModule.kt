package com.baek.diract.di

import com.baek.diract.data.remote.api.ReportApi
import com.baek.diract.data.repository.MyPageRepositoryImpl
import com.baek.diract.domain.repository.MyPageRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MyPageModule {

    @Binds
    @Singleton
    abstract fun bindMyPageRepository(
        impl: MyPageRepositoryImpl
    ): MyPageRepository

    companion object {
        @Provides
        @Singleton
        fun provideReportApi(retrofit: Retrofit): ReportApi {
            return retrofit.create(ReportApi::class.java)
        }
    }
}
