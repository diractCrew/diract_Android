package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Feedback
import com.baek.diract.domain.model.Reply

interface FeedbackRepository {

    //피드백들 가져오기
    suspend fun getFeedbacks(videoId: String): DataResult<List<Feedback>>

    //피드백 작성
    suspend fun uploadFeedback(
        videoId: String,
        taggedUserIds: List<String>,
        content: String,
        startTime: Double,
        endTime: Double? = null,
        teamspaceId: String,
        imageUrl: String? = null
    ): DataResult<Unit>

    //피드백 수정
    suspend fun editFeedback(
        feedbackId: String,
        newContent: String,
        startTime: Double,
        endTime: Double? = null,
        taggedUserIds: List<String> = emptyList()
    ): DataResult<Unit>

    //피드백 삭제
    suspend fun deleteFeedback(feedbackId: String): DataResult<Unit>

    //댓글들 가져오기
    suspend fun getReplies(feedbackId: String): DataResult<List<Reply>>

    //댓글 작성
    suspend fun uploadReply(
        feedbackId: String,
        content: String,
        taggedUserIds: List<String> = emptyList()
    ): DataResult<Unit>

    //댓글 수정
    suspend fun editReply(
        feedbackId: String,
        replyId: String,
        newContent: String,
        taggedUserIds: List<String> = emptyList()
    ): DataResult<Unit>

    //댓글 삭제
    suspend fun deleteReply(feedbackId: String, replyId: String): DataResult<Unit>
}