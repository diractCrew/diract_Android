package com.baek.diract.data.remote.dto

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
