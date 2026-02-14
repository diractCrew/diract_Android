package com.baek.diract.di

import com.baek.diract.data.remote.api.TeamspaceApi
import com.baek.diract.data.repository.TeamspaceRepositoryImpl
import com.baek.diract.domain.repository.TeamspaceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TeamspaceModule {

    @Binds
    @Singleton
    abstract fun bindTeamspaceRepository(
        impl: TeamspaceRepositoryImpl
    ): TeamspaceRepository

    companion object {
        @Provides
        @Singleton
        fun provideTeamspaceApi(retrofit: Retrofit): TeamspaceApi {
            return retrofit.create(TeamspaceApi::class.java)
        }
    }
}