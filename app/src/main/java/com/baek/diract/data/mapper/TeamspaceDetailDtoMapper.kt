package com.baek.diract.data.mapper

import com.baek.diract.data.remote.dto.TeamspaceDetailDto
import com.baek.diract.domain.model.TeamspaceDetail

fun TeamspaceDetailDto.toDomain(): TeamspaceDetail {
    return TeamspaceDetail(
        teamspaceId = teamspaceId,
        teamspaceName = teamspaceName,
        ownerId = ownerId,
        members = members.map { it.toTeamMemberSummary() }
    )
}
