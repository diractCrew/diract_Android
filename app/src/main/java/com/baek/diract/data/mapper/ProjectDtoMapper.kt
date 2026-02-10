package com.baek.diract.data.mapper


import com.baek.diract.data.remote.dto.ProjectDto
import com.baek.diract.domain.model.ProjectSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun ProjectDto.toSummaryDomain(): ProjectSummary = ProjectSummary(
    id = project_id,
    name = project_name,
    teamspaceId = teamspace_id,
    creatorId = creator_id,
    createdAt = created_at?.let {
        Instant.ofEpochSecond(it.seconds)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    } ?: LocalDate.now()
)
