package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.TokenDto
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/login/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): ApiResponse<TokenDto>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): ApiResponse<TokenDto>
}

data class GoogleLoginRequest(@SerializedName("idToken") val idToken: String)

data class RefreshRequest(@SerializedName("refreshToken") val refreshToken: String)
