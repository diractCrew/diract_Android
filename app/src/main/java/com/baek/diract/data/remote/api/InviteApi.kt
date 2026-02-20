package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.InviteDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface InviteApi {
    @POST("api/teamspaces/{teamspaceId}/invites")
    suspend fun createInvite(
        @Path("teamspaceId") teamspaceId: String,
        @Body request: CreateInviteRequest
    ): ApiResponse<InviteDto>

    @POST("api/invites/accept")
    suspend fun acceptInvite(@Body request: AcceptInviteRequest): ApiResponse<Unit>
}

data class CreateInviteRequest(val expiresAt: String)
data class AcceptInviteRequest(val token: String)