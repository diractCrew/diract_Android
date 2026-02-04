package com.baek.diract.domain.model

import java.time.LocalDateTime

data class Reply(
    val replyId: String,
    val feedbackId: String,
    val author: CommentUser,
    val taggedUsers: List<CommentUser>,
    val content: String,
    val updatedAt: LocalDateTime
)
