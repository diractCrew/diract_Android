package com.baek.diract.data.datasource.remote

import com.baek.diract.data.remote.dto.ProjectDto

interface ProjectRemoteDataSource {

    suspend fun getProjects(teamspaceId: String): List<ProjectDto>

    suspend fun createProject(
        teamspaceId: String,
        creatorId: String,
        projectName: String
    ): ProjectDto

    suspend fun editProjectName(
        teamspaceId: String,
        projectId: String,
        newName: String
    ): ProjectDto

    suspend fun deleteProject(
        teamspaceId: String,
        projectId: String
    )
}
