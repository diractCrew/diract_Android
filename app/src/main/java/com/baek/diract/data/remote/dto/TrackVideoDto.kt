package com.baek.diract.data.remote.dto

import com.baek.diract.domain.model.VideoSummary
import java.time.OffsetDateTime

data class TrackVideoDto(
    val trackId: String,
    val videoId: String,
    val sectionId: String,
    val videoTitle: String,
    val thumbnailUrl: String,
    val videoDuration: Double,
    val createdAt: String,
    val updatedAt: String
)

fun TrackVideoDto.toVideoSummary(): VideoSummary = VideoSummary(
    videoId = videoId,
    title = videoTitle,
    duration = videoDuration,
    thumbnailUrl = thumbnailUrl,
    createdAt = OffsetDateTime.parse(createdAt).toLocalDate(),
    trackId = trackId,
    sectionId = sectionId,
    uploaderId = ""
)
