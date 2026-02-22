package com.baek.diract.domain.model

data class TracksSummary(
    val tracksId: String,
    val trackName: String,
    val projectId: String,
    val projectName: String,
    val creatorId: String,
    val creatorName: String,
    val createdAt: String,
    val updatedAt: String,
    val videoCount: Int? = 0
)