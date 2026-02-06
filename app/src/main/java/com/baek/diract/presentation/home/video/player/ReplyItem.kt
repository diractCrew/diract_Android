package com.baek.diract.presentation.home.video.player

import com.baek.diract.domain.model.FeedbackUser
import java.time.LocalDateTime

data class ReplyItem(
    val replyId: String,
    val feedbackId: String,
    val author: FeedbackUser,
    val taggedUsers: List<FeedbackUser> = emptyList(),
    val content: String,
    val createdAt: LocalDateTime,
    val status: CommentStatus = CommentStatus.SUCCESS
)
