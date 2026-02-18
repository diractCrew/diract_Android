package com.baek.diract.data.remote.dto

import com.baek.diract.data.mapper.parseDateTime
import com.baek.diract.domain.model.Feedback

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

fun FeedbackDto.toDomain(): Feedback = Feedback(
    feedbackId = feedbackId,
    videoId = videoId,
    author = authorId,
    taggedUsers = taggedUserIds,
    content = content,
    startTime = startTime.toDouble(),
    endTime = endTime?.toDouble(),
    imgUrl = imageUrl,
    teamspaceId = teamspaceId,
    replyCount = replies.size,
    updatedAt = parseDateTime(updatedAt)
)
