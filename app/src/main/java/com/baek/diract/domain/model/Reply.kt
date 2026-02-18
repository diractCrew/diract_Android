package com.baek.diract.domain.model

import java.time.LocalDateTime

data class Reply(
    val replyId: String,
    val feedbackId: String,
    val author: String,
    val taggedUsers: List<String> = emptyList(),
    val content: String,
    val updatedAt: LocalDateTime
)
