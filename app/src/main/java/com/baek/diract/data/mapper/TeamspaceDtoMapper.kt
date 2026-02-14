package com.baek.diract.data.mapper

import com.baek.diract.data.remote.dto.TeamspaceDto
import com.baek.diract.domain.model.TeamspaceSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun TeamspaceDto.toTeamspaceSummary(): TeamspaceSummary =
    TeamspaceSummary(
        id = teamspaceId,
        name = teamspaceName,
        ownerId = ownerId,
        createdAt = createdAt?.let { iso ->
            runCatching {
                Instant.parse(iso)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }.getOrElse { LocalDate.now() }
        } ?: LocalDate.now()
    )