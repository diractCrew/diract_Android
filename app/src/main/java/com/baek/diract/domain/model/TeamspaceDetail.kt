package com.baek.diract.domain.model

data class TeamspaceDetail(
    val teamspaceId: String,
    val teamspaceName: String,
    val ownerId: String,
    val members: List<TeamMemberSummary>
)
