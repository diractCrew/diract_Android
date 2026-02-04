package com.baek.diract.domain.model

import java.time.LocalDateTime

data class Reply(
    val replyId: String,
    val feedbackId: String,
    val author: FeedbackUser,
    val taggedUsers: List<FeedbackUser>,
    val content: String,
    val updatedAt: LocalDateTime
)
