package com.baek.diract.data.mapper

import com.baek.diract.data.remote.dto.TeamspaceDto
import com.baek.diract.domain.model.TeamspaceSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun TeamspaceDto.toDomain(): TeamspaceSummary = TeamspaceSummary(
    id = teamspace_id,
    name = teamspace_name,
    ownerId = owner_id,
    createdAt = created_at?.let {
        Instant.ofEpochSecond(it.seconds)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    } ?: LocalDate.now()
)