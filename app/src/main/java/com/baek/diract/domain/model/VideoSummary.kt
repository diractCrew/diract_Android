package com.baek.diract.domain.model

import java.time.LocalDateTime

data class VideoSummary(
    val trackId: String,
    val videoId: String,
    val sectionId: String,
    val title: String,
    val thumbnailUrl: String,
    val duration: Double, //초단위
    val createdAt: LocalDateTime,
    val isMyVideo: Boolean
)
