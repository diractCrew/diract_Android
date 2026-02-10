package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.UserDto
import retrofit2.http.GET

interface UserApi {

    @GET("api/users/me")
    suspend fun getMe(): ApiResponse<UserDto>
}
