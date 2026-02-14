// di/ProjectModule.kt
package com.baek.diract.di

import com.baek.diract.data.remote.api.ProjectApi
import com.baek.diract.data.repository.ProjectRepositoryImpl
import com.baek.diract.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectBindModule {
    @Binds
    abstract fun bindProjectRepository(
        impl: ProjectRepositoryImpl
    ): ProjectRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ProjectProvideModule {
    @Provides
    @Singleton
    fun provideProjectApi(retrofit: Retrofit): ProjectApi =
        retrofit.create(ProjectApi::class.java)
}
