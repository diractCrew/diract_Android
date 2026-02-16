package com.baek.diract.data.remote.dto

data class SectionDto(
    val sectionId: String,
    val sectionName: String,
    val tracksId: String,
    val createdAt: String,
    val updatedAt: String,
    val tracks: List<TrackDto>
)
