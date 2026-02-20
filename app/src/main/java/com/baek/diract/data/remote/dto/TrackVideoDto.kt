package com.baek.diract.data.remote.dto

import com.baek.diract.data.mapper.parseDateTime
import com.baek.diract.domain.model.VideoSummary
import java.time.OffsetDateTime

data class TrackVideoDto(
    val trackId: String,
    val videoId: String,
    val sectionId: String,
    val videoTitle: String,
    val thumbnailUrl: String,
    val videoDuration: Double,
    val isMyVideo: Boolean,
    val createdAt: String,
    val updatedAt: String
)

fun TrackVideoDto.toVideoSummary(): VideoSummary = VideoSummary(
    trackId = trackId,
    videoId = videoId,
    sectionId = sectionId,
    title = videoTitle,
    thumbnailUrl = thumbnailUrl,
    duration = videoDuration,
    createdAt = parseDateTime(createdAt),
    isMyVideo = isMyVideo
)
