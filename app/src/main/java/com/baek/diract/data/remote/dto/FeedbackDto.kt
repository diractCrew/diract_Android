package com.baek.diract.data.remote.dto

data class FeedbackDto(
    val feedbackId: String,
    val content: String,
    val authorId: String,
    val videoId: String,
    val teamspaceId: String,
    val startTime: Int,
    val endTime: Int? = null,
    val imageUrl: String,
    val taggedUserIds: List<String>,
    val createdAt: String,
    val updatedAt: String,
    val replies: List<ReplyDto> = emptyList()
)
