package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.TrackDto
import com.baek.diract.data.remote.dto.TracksDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface TracksApi {
    @POST("api/projects/{projectId}/tracks")
    suspend fun createTracks(
        @Path("projectId") projectId: String,
        @Body request: TracksNameRequest
    ): ApiResponse<TracksDto>

    @GET("api/projects/{projectId}/tracks")
    suspend fun getTracksList(
        @Path("projectId") projectId: String
    ): ApiResponse<List<TracksDto>>

    @PUT("api/tracks/{tracksId}")
    suspend fun updateTracks(
        @Path("tracksId") tracksId: String,
        @Body request: TracksNameRequest
    ): ApiResponse<TracksDto>

    @DELETE("api/tracks/{tracksId}")
    suspend fun deleteTracks(
        @Path("tracksId") tracksId: String
    ): ApiResponse<Unit?>
}

data class TracksNameRequest(
    val trackName: String
)