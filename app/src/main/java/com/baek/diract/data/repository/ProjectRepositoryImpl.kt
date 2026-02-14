package com.baek.diract.data.repository

import com.baek.diract.data.mapper.toSummaryDomain
import com.baek.diract.data.remote.api.CreateProjectRequest
import com.baek.diract.data.remote.api.ProjectApi
import com.baek.diract.data.remote.api.UpdateProjectRequest
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.ProjectSummary
import com.baek.diract.domain.repository.ProjectRepository
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectApi: ProjectApi
) : ProjectRepository {

    override suspend fun getTeamspaceProjects(teamspaceId: String): DataResult<List<ProjectSummary>> =
        runCatching {
            val res = projectApi.getTeamspaceProjects(teamspaceId)
            if (!res.success) throw IllegalStateException(res.message ?: "프로젝트 목록 조회 실패")
            val dtoList = res.data ?: emptyList()
            dtoList.map { it.toSummaryDomain() }
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Error(it) }
        )

    override suspend fun getMyProjects(): DataResult<List<ProjectSummary>> =
        runCatching {
            val res = projectApi.getMyProjects()
            if (!res.success) throw IllegalStateException(res.message ?: "내 프로젝트 목록 조회 실패")
            val dtoList = res.data ?: emptyList()
            dtoList.map { it.toSummaryDomain() }
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Error(it) }
        )

    override suspend fun createProject(teamspaceId: String, projectName: String): DataResult<ProjectSummary> =
        runCatching {
            val res = projectApi.createProject(teamspaceId, CreateProjectRequest(projectName))
            if (!res.success) throw IllegalStateException(res.message ?: "프로젝트 생성 실패")
            val dto = res.data ?: throw IllegalStateException("프로젝트 생성 응답 data가 null")
            dto.toSummaryDomain()
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Error(it) }
        )

    override suspend fun editProjectName(projectId: String, newName: String): DataResult<ProjectSummary> =
        runCatching {
            val res = projectApi.updateProject(projectId, UpdateProjectRequest(newName))
            if (!res.success) throw IllegalStateException(res.message ?: "프로젝트 수정 실패")
            val dto = res.data ?: throw IllegalStateException("프로젝트 수정 응답 data가 null")
            dto.toSummaryDomain()
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Error(it) }
        )

    override suspend fun deleteProject(projectId: String): DataResult<Unit> =
        runCatching {
            val res = projectApi.deleteProject(projectId)
            if (!res.success) throw IllegalStateException(res.message ?: "프로젝트 삭제 실패")
            Unit
        }.fold(
            onSuccess = { DataResult.Success(Unit) },
            onFailure = { DataResult.Error(it) }
        )
}
