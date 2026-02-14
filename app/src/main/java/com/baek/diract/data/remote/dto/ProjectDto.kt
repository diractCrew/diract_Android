package com.baek.diract.data.remote.dto


data class ProjectDto(
    val projectId: String,
    val projectName: String,
    val creatorId: String? = null,
    val creatorName: String? = null,
    val teamspaceId: String? = null,
    val teamspaceName: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)