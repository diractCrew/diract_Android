package com.baek.diract.data.remote.dto

import com.baek.diract.domain.model.VideoPlay

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

fun VideoDto.toPlayDomain(): VideoPlay = VideoPlay(
    id = videoId,
    videoTitle = videoTitle,
    videoDuration = videoDuration.toDouble(),
    videoUrl = videoUrl,
    uploaderId = uploaderId
)
