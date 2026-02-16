package com.baek.diract.data.remote.dto

data class VideoDto(
    val videoId: String,
    val videoTitle: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val uploaderId: String,
    val videoDuration: Int,
    val createdAt: String,
    val updatedAt: String
)
