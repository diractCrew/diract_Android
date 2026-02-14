package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApi {

    @GET("api/users/me")
    suspend fun getMe(): ApiResponse<UserDto>

    @PATCH("api/users/me")
    suspend fun editMe(@Body request: EditMeRequest): ApiResponse<UserDto>

    @DELETE("api/users/me")
    suspend fun deleteMe(): ApiResponse<Unit>
}

data class EditMeRequest(
    val name: String? = null,
    val fcmToken: String? = null,
    val termsAgreed: Boolean? = null,
    val privacyAgreed: Boolean? = null,
)
