package com.baek.diract.data.mapper

import com.baek.diract.data.remote.dto.TracksDto
import com.baek.diract.domain.model.TracksSummary

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
