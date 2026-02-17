package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.FeedbackDto
import com.baek.diract.data.remote.dto.ReplyDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface FeedbackApi {

    //피드백 생성 API
    @POST("api/videos/{videoId}/feedbacks")
    suspend fun createFeedback(
        @Path("videoId") videoId: String,
        @Body request: CreateFeedbackRequest
    ): ApiResponse<FeedbackDto>

    //피드백 단건 조회 API
    @GET("/api/feedbacks/{feedbackId}")
    suspend fun getFeedbackWithReplies(
        @Path("feedbackId") feedbackId: String
    ): ApiResponse<FeedbackDto>

    //특정 비디오의 피드백 목록 조회 API
    @GET("api/videos/{videoId}/feedbacks")
    suspend fun getFeedbackList(
        @Path("videoId") videoId: String
    ): ApiResponse<List<FeedbackDto>>

    //피드백 수정 API
    @PATCH("api/feedbacks/{feedbackId}")
    suspend fun editFeedback(
        @Path("feedbackId") feedbackId: String,
        @Body request: EditFeedbackRequest
    ): ApiResponse<FeedbackDto>

    //피드백 삭제 API
    @DELETE("api/feedbacks/{feedbackId}")
    suspend fun deleteFeedback(
        @Path("feedbackId") feedbackId: String
    ): ApiResponse<Unit>

    //답글 생성 API
    @POST("api/feedbacks/{feedbackId}/replies")
    suspend fun createReply(
        @Path("feedbackId") feedbackId: String,
        @Body request: CreateReplyRequest
    ): ApiResponse<ReplyDto>

    //답글 목록 조회 API
    @GET("api/feedbacks/{feedbackId}/replies")
    suspend fun getReplyList(
        @Path("feedbackId") feedbackId: String
    ): ApiResponse<List<ReplyDto>>

    //답글 수정 API
    @PATCH("api/feedbacks/{feedbackId}/replies/{replyId}")
    suspend fun editReply(
        @Path("feedbackId") feedbackId: String,
        @Path("replyId") replyId: String,
        @Body request: EditReplyRequest
    ): ApiResponse<ReplyDto>

    //답글 삭제 API
    @DELETE("api/feedbacks/{feedbackId}/replies/{replyId}")
    suspend fun deleteReply(
        @Path("feedbackId") feedbackId: String,
        @Path("replyId") replyId: String
    ): ApiResponse<Unit>
}

data class CreateFeedbackRequest(
    val content: String,
    val videoId: String,
    val teamspaceId: String,
    val startTime: Int,
    val endTime: Int?,
    val imageUrl: String? = null,
    val taggedUserIds: List<String> = emptyList()
)

data class EditFeedbackRequest(
    val content: String,
    val startTime: Int,
    val endTime: Int? = null,
    val imageUrl: String? = null,
    val taggedUserIds: List<String> = emptyList()
)

data class CreateReplyRequest(
    val content: String,
    val taggedUserIds: List<String> = emptyList()
)

data class EditReplyRequest(
    val content: String,
    val taggedUserIds: List<String> = emptyList()
)
