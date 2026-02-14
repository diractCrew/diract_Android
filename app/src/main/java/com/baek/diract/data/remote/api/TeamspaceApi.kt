package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.TeamspaceDetailDto
import com.baek.diract.data.remote.dto.TeamspaceDto
import com.baek.diract.data.remote.dto.TeamspaceMemberDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface TeamspaceApi{
    @POST("api/teamspaces")
    suspend fun createTeamspace(@Body request: CreateTeamspaceRequest): ApiResponse<TeamspaceDto>

    @GET("api/teamspaces")
    suspend fun getMyTeamspaces(): ApiResponse<List<TeamspaceDto>>

    @GET("api/teamspaces/{teamspaceId}")
    suspend fun getTeamspaceDetail(
        @Path("teamspaceId") teamspaceId: String
    ): ApiResponse<TeamspaceDetailDto>


    @PATCH("api/teamspaces/{teamspaceId}")
    suspend fun updateTeamspace(@Path("teamspaceId") teamspaceId: String, @Body request: UpdateTeamspaceRequest): ApiResponse<TeamspaceDto>

    @DELETE("api/teamspaces/{teamspaceId}")
    suspend fun deleteTeamspace(@Path("teamspaceId") teamspaceId: String): ApiResponse<Unit>

    @POST("api/teamspaces/{teamspaceId}/members")
    suspend fun addMember(@Path("teamspaceId") teamspaceId: String,@Body request: AddMemberRequest): ApiResponse<Unit>

    @DELETE("api/teamspaces/{teamspaceId}/members/{userId}")
    suspend fun deleteMember( @Path("teamspaceId") teamspaceId: String,@Path("userId") userId: String): ApiResponse<Unit>

    @GET("api/teamspaces/{teamspaceId}/members")
    suspend fun getMembers(@Path("teamspaceId") teamspaceId: String): ApiResponse<List<TeamspaceMemberDto>>

}
data class CreateTeamspaceRequest(val teamspaceName: String)
data class UpdateTeamspaceRequest(val teamspaceName: String)
data class AddMemberRequest(val userId: String)
