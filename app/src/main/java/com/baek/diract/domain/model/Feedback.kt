package com.baek.diract.domain.model

import com.baek.diract.presentation.home.video.player.FeedbackItem
import java.time.LocalDateTime

data class Feedback(
    val feedbackId: String,
    val videoId: String,
    val author: CommentUser,
    val taggedUsers: List<CommentUser> = emptyList(),
    val content: String,
    val startTime: Double,
    val endTime: Double? = null,
    val imgUrl: String? = null,
    val teamspaceId: String,
    val replyCount: Int = 0,
    val updatedAt: LocalDateTime
) {
    fun toUiItem(): FeedbackItem {
        return FeedbackItem(
            feedbackId = feedbackId,
            videoId = videoId,
            author = author,
            taggedUsers = taggedUsers,
            content = content,
            startTime = startTime,
            endTime = endTime,
            imgUrl = imgUrl,
            teamspaceId = teamspaceId,
            replyCount = replyCount,
            updatedAt = updatedAt
        )

    }
}
