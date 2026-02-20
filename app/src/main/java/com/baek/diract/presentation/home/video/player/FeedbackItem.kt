package com.baek.diract.presentation.home.video.player

import com.baek.diract.domain.model.TeamMemberSummary
import java.time.LocalDateTime

data class FeedbackItem(
    val feedbackId: String,
    val videoId: String,
    val author: TeamMemberSummary,
    val taggedUsers: List<TeamMemberSummary> = emptyList(),
    val content: String,
    val startTime: Double,
    val endTime: Double? = null,
    val imgUrl: String? = null,
    val teamspaceId: String,
    val replyCount: Int = 0,
    val updatedAt: LocalDateTime,
    val status: CommentStatus = CommentStatus.SUCCESS
)

enum class CommentStatus {
    LOADING, SUCCESS, FAIL
}
