package com.baek.diract.data.remote.dto

import com.baek.diract.domain.model.TracksSummary
import com.google.firebase.Timestamp

data class TracksDto(
    val tracksId: String,
    val trackName: String,
    val projectId: String,
    val projectName: String,
    val creatorId: String,
    val creatorName: String,
    val createdAt: String,
    val updatedAt: String
)
