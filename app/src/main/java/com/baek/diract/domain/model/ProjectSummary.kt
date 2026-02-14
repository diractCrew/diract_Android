package com.baek.diract.domain.model

import java.time.LocalDate

data class ProjectSummary(
    val id: String,
    val name: String,
    val teamspaceId: String,
    val creatorId: String,
    val createdAt: LocalDate
)