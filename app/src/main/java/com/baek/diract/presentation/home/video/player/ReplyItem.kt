package com.baek.diract.presentation.home.video.player

import com.baek.diract.domain.model.TeamMemberSummary
import java.time.LocalDateTime

data class ReplyItem(
    val replyId: String,
    val feedbackId: String,
    val author: TeamMemberSummary,
    val taggedUsers: List<TeamMemberSummary> = emptyList(),
    val content: String,
    val createdAt: LocalDateTime,
    val status: CommentStatus = CommentStatus.SUCCESS
)