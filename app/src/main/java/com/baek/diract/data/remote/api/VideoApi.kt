package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.VideoDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface VideoApi {

    // 업로드 URL 발급
    @POST("api/videos/upload-url")
    suspend fun getUploadUrl(): ApiResponse<UploadUrlResponse>

    // 비디오 메타데이터 저장
    @POST("api/videos")
    suspend fun createVideo(
        @Body request: CreateVideoRequest
    ): ApiResponse<VideoDto>

    // 비디오 단건 조회
    @GET("api/videos/{videoId}")
    suspend fun getVideo(
        @Path("videoId") videoId: String
    ): ApiResponse<VideoDto>

    // 비디오 수정
    @PATCH("api/videos/{videoId}")
    suspend fun editVideo(
        @Path("videoId") videoId: String,
        @Body request: EditVideoRequest
    ): ApiResponse<VideoDto>

    // 비디오 삭제
    @DELETE("api/videos/{videoId}")
    suspend fun deleteVideo(
        @Path("videoId") videoId: String
    ): ApiResponse<Unit>
}

data class UploadUrlResponse(
    val videoId: String,
    val videoUploadUrl: String,
    val thumbnailUploadUrl: String
)

data class CreateVideoRequest(
    val videoId: String,
    val videoTitle: String,
    val videoDuration: Double
)

data class EditVideoRequest(
    val videoTitle: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val videoDuration: Double
)
