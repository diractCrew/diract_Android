package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.TrackDto
import com.baek.diract.data.remote.dto.TrackVideoDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface TrackApi {

    //트랙(Track) 생성 API
    @POST("api/tracks/{tracksId}/sections/{sectionId}/track")
    suspend fun createTrack(
        @Path("tracksId") tracksId: String,
        @Path("sectionId") sectionId: String,
        @Body request: CreateTrackRequest
    ): ApiResponse<TrackDto>

    //트랙(Track) 목록 조회 API
    @GET("api/tracks/{tracksId}/sections/{sectionId}/track")
    suspend fun getTrackList(
        @Path("tracksId") tracksId: String,
        @Path("sectionId") sectionId: String,
    ): ApiResponse<List<TrackVideoDto>>

    //트랙(Track) 수정 API
    @PATCH("api/tracks/{tracksId}/sections/{sectionId}/track/{trackId}")
    suspend fun editTrack(
        @Path("tracksId") tracksId: String,
        @Path("sectionId") sectionId: String,
        @Path("trackId") trackId: String,
        @Body request: EditTrackRequest
    ): ApiResponse<TrackDto>

    //트랙(Track) 삭제 API
    @DELETE("api/tracks/{tracksId}/sections/{sectionId}/track/{trackId}")
    suspend fun deleteTrack(
        @Path("tracksId") tracksId: String,
        @Path("sectionId") sectionId: String,
        @Path("trackId") trackId: String
    ): ApiResponse<Unit>
}

data class CreateTrackRequest(
    val videoId: String
)

data class EditTrackRequest(
    val videoId: String
)
