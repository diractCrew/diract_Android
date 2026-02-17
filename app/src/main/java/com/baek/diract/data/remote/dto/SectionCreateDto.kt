package com.baek.diract.data.remote.dto

import com.baek.diract.domain.model.Section

data class SectionCreateDto (
    val sectionId: String,
    val sectionName: String,
    val tracksId: String,
    val createdAt: String,
    val updatedAt: String
)

fun SectionCreateDto.toDomain(): Section = Section(
    id = sectionId,
    title = sectionName
)
