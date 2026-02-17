package com.baek.diract.data.remote.dto

data class ReplyDto(
    val replyId: String,
    val content: String,
    val authorId: String,
    val feedbackId: String,
    val taggedUserIds: List<String> = emptyList(),
    val createAt: String,
    val updatedAt: String
)
