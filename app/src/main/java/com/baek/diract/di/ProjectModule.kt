// di/ProjectModule.kt
package com.baek.diract.di

import com.baek.diract.data.datasource.remote.ProjectRemoteDataSource
import com.baek.diract.data.datasource.remote.ProjectRemoteDataSourceImpl
import com.baek.diract.data.repository.ProjectRepositoryImpl
import com.baek.diract.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectModule {

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        impl: ProjectRepositoryImpl
    ): ProjectRepository

//    @Binds
//    @Singleton
//    abstract fun bindProjectRemoteDataSource(
//        impl: ProjectRemoteDataSourceImpl
//    ): ProjectRemoteDataSource
}
