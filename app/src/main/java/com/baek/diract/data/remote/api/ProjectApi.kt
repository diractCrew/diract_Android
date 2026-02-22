package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.ProjectDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ProjectApi {

    // 팀스페이스에 프로젝트 생성
    @POST("api/teamspaces/{teamspaceId}/projects")
    suspend fun createProject(
        @Path("teamspaceId") teamspaceId: String,
        @Body request: CreateProjectRequest
    ): ApiResponse<ProjectDto>

    // 프로젝트 상세 조회
    @GET("api/projects/{projectId}")
    suspend fun getProjectDetail(
        @Path("projectId") projectId: String
    ): ApiResponse<ProjectDto>

    // 팀스페이스의 프로젝트 목록 조회
    @GET("api/teamspaces/{teamspaceId}/projects")
    suspend fun getTeamspaceProjects(
        @Path("teamspaceId") teamspaceId: String
    ): ApiResponse<List<ProjectDto>>

    // 내가 만든 프로젝트 목록 조회
    @GET("api/projects")
    suspend fun getMyProjects(): ApiResponse<List<ProjectDto>>

    // 프로젝트 수정
    @PATCH("api/projects/{projectId}")
    suspend fun updateProject(
        @Path("projectId") projectId: String,
        @Body request: UpdateProjectRequest
    ): ApiResponse<ProjectDto>

    // 프로젝트 삭제
    @DELETE("api/projects/{projectId}")
    suspend fun deleteProject(
        @Path("projectId") projectId: String
    ): ApiResponse<Unit>
}
data class CreateProjectRequest(
    val projectName: String
)

data class UpdateProjectRequest(
    val projectName: String
)