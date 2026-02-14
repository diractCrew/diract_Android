// domain/repository/ProjectRepository.kt
package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.ProjectSummary

interface ProjectRepository {

    // 팀스페이스의 프로젝트 목록 조회
    suspend fun getProjects(teamspaceId: String): DataResult<List<ProjectSummary>>

    // 프로젝트 생성
    suspend fun createProject(
        teamspaceId: String,
        creatorId: String,
        projectName: String
    ): DataResult<ProjectSummary>

    // 프로젝트 이름 수정
    suspend fun editProjectName(
        teamspaceId: String,
        projectId: String,
        newName: String
    ): DataResult<ProjectSummary>

    // 프로젝트 삭제
    suspend fun deleteProject(
        teamspaceId: String,
        projectId: String
    ): DataResult<Unit>
}
