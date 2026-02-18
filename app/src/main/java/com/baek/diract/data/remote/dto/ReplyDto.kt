package com.baek.diract.data.remote.dto

import com.baek.diract.data.mapper.parseDateTime
import com.baek.diract.domain.model.Reply

data class ReplyDto(
    val replyId: String,
    val content: String,
    val authorId: String,
    val feedbackId: String,
    val taggedUserIds: List<String> = emptyList(),
    val createAt: String,
    val updatedAt: String
)

fun ReplyDto.toDomain(): Reply = Reply(
    replyId = replyId,
    feedbackId = feedbackId,
    author = authorId,
    taggedUsers = taggedUserIds,
    content = content,
    updatedAt = parseDateTime(updatedAt)
)
