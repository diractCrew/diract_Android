package com.baek.diract.data.remote.dto

data class TeamspaceDetailDto(
    val teamspaceId: String = "",
    val teamspaceName: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val members: List<TeamspaceMemberDto> = emptyList(),
    val memberCount: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
