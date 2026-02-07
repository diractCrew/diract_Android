package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.FeedbackUser
import com.baek.diract.domain.model.Feedback
import com.baek.diract.domain.model.Reply

interface FeedbackRepository {

    //팀스페이스 유저들 가져오기
    suspend fun getTeamspaceUsers(teamspaceId: String): DataResult<List<FeedbackUser>>

    //피드백들 가져오기
    suspend fun getFeedbacks(videoId: String): DataResult<List<Feedback>>

    //피드백 작성
    suspend fun uploadFeedback(
        videoId: String,
        authorId: String,
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
        taggedUserIds: List<String> = emptyList()
    ): DataResult<Unit>

    //피드백 삭제
    suspend fun deleteFeedback(feedbackId: String): DataResult<Unit>

    //피드백 신고
    suspend fun reportFeedback(feedbackId: String): DataResult<Unit>

    //댓글들 가져오기
    suspend fun getReplies(feedbackId: String): DataResult<List<Reply>>

    //댓글 작성
    suspend fun uploadReply(reply: Reply): DataResult<Unit>

    //댓글 수정
    suspend fun editReply(replyId: String, newContent: String, taggedUserIds: List<String> = emptyList()): DataResult<Unit>

    //댓글 삭제
    suspend fun deleteReply(replyId: String): DataResult<Unit>

    //댓글 신고
    suspend fun reportReply(replyId: String): DataResult<Unit>
}
