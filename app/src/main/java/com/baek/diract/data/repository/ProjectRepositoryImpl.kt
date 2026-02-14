package com.baek.diract.data.repository

import com.baek.diract.data.datasource.remote.ProjectRemoteDataSource
import com.baek.diract.data.mapper.toSummaryDomain
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.ProjectSummary
import com.baek.diract.domain.repository.ProjectRepository
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val remote: ProjectRemoteDataSource
) : ProjectRepository {

    override suspend fun getProjects(teamspaceId: String): DataResult<List<ProjectSummary>> =
        runCatching { remote.getProjects(teamspaceId).map { it.toSummaryDomain() } }
            .fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Error(it) }
            )

    override suspend fun createProject(
        teamspaceId: String,
        creatorId: String,
        projectName: String
    ): DataResult<ProjectSummary> =
        runCatching { remote.createProject(teamspaceId, creatorId, projectName).toSummaryDomain() }
            .fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Error(it) }
            )

    override suspend fun editProjectName(
        teamspaceId: String,
        projectId: String,
        newName: String
    ): DataResult<ProjectSummary> =
        runCatching { remote.editProjectName(teamspaceId, projectId, newName).toSummaryDomain() }
            .fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Error(it) }
            )

    override suspend fun deleteProject(teamspaceId: String, projectId: String): DataResult<Unit> =
        runCatching { remote.deleteProject(teamspaceId, projectId) }
            .fold(
                onSuccess = { DataResult.Success(Unit) },
                onFailure = { DataResult.Error(it) }
            )
}
