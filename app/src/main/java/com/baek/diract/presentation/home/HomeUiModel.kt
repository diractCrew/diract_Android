package com.baek.diract.presentation.home

import com.baek.diract.domain.model.ProjectSummary
import com.baek.diract.domain.model.TeamspaceSummary

data class HomeUiModel(
    val isLoading: Boolean = false,
    val selectedTeamspace: TeamspaceSummary? = null, // ✅ TeamspaceUi 제거
    val role: Role = Role.MEMBER,
    val projects: List<ProjectSummary> = emptyList(),
    val tracksCountByProjectId: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null
) {
    val hasTeamspace: Boolean get() = selectedTeamspace != null
    val hasProjects: Boolean get() = projects.isNotEmpty()
}

enum class Role { OWNER, MEMBER }
