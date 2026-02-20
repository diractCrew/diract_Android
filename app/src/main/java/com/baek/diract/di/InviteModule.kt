package com.baek.diract.di

import com.baek.diract.data.remote.api.InviteApi
import com.baek.diract.data.repository.InviteRepositoryImpl
import com.baek.diract.domain.repository.InviteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InviteModule {

    @Binds
    @Singleton
    abstract fun bindInviteRepository(
        impl: InviteRepositoryImpl
    ): InviteRepository

    companion object {
        @Provides
        @Singleton
        fun provideInviteApi(retrofit: Retrofit): InviteApi {
            return retrofit.create(InviteApi::class.java)
        }
    }
}
