package com.baek.diract.domain.model

data class VideoPlay(
    val id: String,
    val videoTitle: String,
    val videoDuration: Double,
    val videoUrl: String,
    val uploaderId: String
)
