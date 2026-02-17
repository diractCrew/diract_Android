package com.baek.diract.data.remote.dto

import com.baek.diract.domain.model.Section

data class SectionDto(
    val sectionId: String,
    val sectionName: String,
    val tracksId: String,
    val createdAt: String,
    val updatedAt: String,
    val tracks: List<TrackSummaryDto>
)

fun SectionDto.toDomain(): Section = Section(
    id = sectionId,
    title = sectionName
)
