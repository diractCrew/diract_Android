package com.baek.diract.domain.model

import java.time.LocalDate

data class VideoSummary(
    val id: String,
    val title: String,
    val duration: Double, //초단위
    val thumbnailUrl: String,
    val createdAt: LocalDate,
    val trackId: String,
    val sectionId: String,
    val uploaderId: String
)
