package com.baek.diract.data.mapper


import com.baek.diract.data.remote.dto.ProjectDto
import com.baek.diract.domain.model.ProjectSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun ProjectDto.toSummaryDomain(): ProjectSummary = ProjectSummary(
    id = projectId,
    name = projectName,
    teamspaceId = teamspaceId.orEmpty(),
    creatorId = creatorId.orEmpty(),
    createdAt = createdAt?.let { iso ->
        runCatching {
            Instant.parse(iso)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.getOrNull()
    } ?: LocalDate.now()
)