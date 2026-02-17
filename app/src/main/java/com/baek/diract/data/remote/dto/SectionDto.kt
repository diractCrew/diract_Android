package com.baek.diract.data.remote.dto

import com.baek.diract.domain.model.Section

data class SectionDto(
    val sectionId: String,
    val sectionTitle: String,
    val tracks: List<TrackSummaryDto>,
    val trackCount: Int,
    val createdAt: String,
    val updatedAt: String
)

fun SectionDto.toDomain(): Section = Section(
    id = sectionId,
    title = sectionTitle
)
