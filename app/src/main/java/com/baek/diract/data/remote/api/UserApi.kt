package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApi {

    @GET("api/users/me")
    suspend fun getMe(): ApiResponse<UserDto>

    @PATCH("api/users/me")
    suspend fun updateMe(@Body request: UpdateMeRequest): ApiResponse<UserDto>
}

data class UpdateMeRequest(val name: String)
