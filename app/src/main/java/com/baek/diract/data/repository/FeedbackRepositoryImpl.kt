package com.baek.diract.data.repository

import android.util.Log
import com.baek.diract.data.remote.api.CreateFeedbackRequest
import com.baek.diract.data.remote.api.CreateReplyRequest
import com.baek.diract.data.remote.api.EditFeedbackRequest
import com.baek.diract.data.remote.api.EditReplyRequest
import com.baek.diract.data.remote.api.FeedbackApi
import com.baek.diract.data.remote.dto.toDomain
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Feedback
import com.baek.diract.domain.model.Reply
import com.baek.diract.domain.repository.FeedbackRepository
import javax.inject.Inject

class FeedbackRepositoryImpl @Inject constructor(
    private val feedbackApi: FeedbackApi
) : FeedbackRepository {

    override suspend fun getFeedbacks(videoId: String): DataResult<List<Feedback>> = safeCall {
        feedbackApi.getFeedbackList(videoId).data?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun uploadFeedback(
        videoId: String,
        taggedUserIds: List<String>,
        content: String,
        startTime: Double,
        endTime: Double?,
        teamspaceId: String,
        imageUrl: String?
    ): DataResult<Unit> = safeCall {
        feedbackApi.createFeedback(
            videoId = videoId,
            request = CreateFeedbackRequest(
                content = content,
                videoId = videoId,
                teamspaceId = teamspaceId,
                startTime = startTime.toInt(),
                endTime = endTime?.toInt(),
                imageUrl = imageUrl,
                taggedUserIds = taggedUserIds
            )
        )
    }

    override suspend fun editFeedback(
        feedbackId: String,
        newContent: String,
        startTime: Double,
        endTime: Double?,
        taggedUserIds: List<String>
    ): DataResult<Unit> = safeCall {
        feedbackApi.editFeedback(
            feedbackId = feedbackId,
            request = EditFeedbackRequest(
                content = newContent,
                startTime = startTime.toInt(),
                endTime = endTime?.toInt(),
                taggedUserIds = taggedUserIds
            )
        )
    }

    override suspend fun deleteFeedback(feedbackId: String): DataResult<Unit> = safeCall {
        feedbackApi.deleteFeedback(feedbackId)
    }

    override suspend fun getReplies(feedbackId: String): DataResult<List<Reply>> = safeCall {
        feedbackApi.getReplyList(feedbackId).data?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun uploadReply(
        feedbackId: String,
        content: String,
        taggedUserIds: List<String>
    ): DataResult<Unit> = safeCall {
        feedbackApi.createReply(
            feedbackId = feedbackId,
            request = CreateReplyRequest(
                content = content,
                taggedUserIds = taggedUserIds
            )
        )
    }

    override suspend fun editReply(
        feedbackId: String,
        replyId: String,
        newContent: String,
        taggedUserIds: List<String>
    ): DataResult<Unit> = safeCall {
        feedbackApi.editReply(
            feedbackId = feedbackId,
            replyId = replyId,
            request = EditReplyRequest(
                content = newContent,
                taggedUserIds = taggedUserIds
            )
        )
    }

    override suspend fun deleteReply(feedbackId: String, replyId: String): DataResult<Unit> =
        safeCall {
            feedbackApi.deleteReply(feedbackId = feedbackId, replyId = replyId)
        }

    private inline fun <T> safeCall(block: () -> T): DataResult<T> {
        return try {
            DataResult.Success(block())
        } catch (e: Exception) {
            val serverMessage = (e as? retrofit2.HttpException)
                ?.response()?.errorBody()?.string()
            Log.e(TAG, "serverMessage=$serverMessage", e)
            DataResult.Error(e)
        }
    }

    companion object {
        const val TAG = "FeedbackRepository"
    }
}
