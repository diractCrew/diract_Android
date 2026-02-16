package com.baek.diract.data.mapper

import com.baek.diract.data.dto.*
import com.baek.diract.data.remote.dto.TracksDto
import com.baek.diract.domain.model.*

fun TracksDto.toDomain(): TracksSummary = TracksSummary(
    tracksId = tracksId,  // ✅
    trackName = trackName,
    projectId = projectId,
    projectName = projectName,
    creatorId = creatorId,
    creatorName = creatorName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

