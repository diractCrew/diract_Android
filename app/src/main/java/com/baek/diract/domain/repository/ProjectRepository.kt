// domain/repository/ProjectRepository.kt
package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.ProjectSummary


interface ProjectRepository {
    suspend fun getTeamspaceProjects(teamspaceId: String): DataResult<List<ProjectSummary>>
    suspend fun getMyProjects(): DataResult<List<ProjectSummary>>
    suspend fun createProject(teamspaceId: String, projectName: String): DataResult<ProjectSummary>
    suspend fun editProjectName(projectId: String, newName: String): DataResult<ProjectSummary>
    suspend fun deleteProject(projectId: String): DataResult<Unit>
}
