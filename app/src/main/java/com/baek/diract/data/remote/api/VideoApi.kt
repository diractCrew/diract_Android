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

    //비디오 생성 API
    @POST("api/videos")
    suspend fun createVideo(
        @Body request: CreateVideoRequest
    ): ApiResponse<VideoDto>

    //비디오 단건 조회 API
    @GET("api/videos/{videoId}")
    suspend fun getVideo(
        @Path("videoId") videoId: String
    ): ApiResponse<VideoDto>

    //비디오 수정 API
    @PATCH("api/videos/{videoId}")
    suspend fun editVideo(
        @Path("videoId") videoId: String,
        @Body request: EditVideoRequest
    ): ApiResponse<VideoDto>

    //비디오 삭제 API
    @DELETE("api/videos/{videoId}")
    suspend fun deleteVideo(
        @Path("videoId") videoId: String
    ): ApiResponse<Unit>
}

data class CreateVideoRequest(
    val videoTitle: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val videoDuration: Int
)

data class EditVideoRequest(
    val videoTitle: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val videoDuration: Int
)
