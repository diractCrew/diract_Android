package com.baek.diract.data.remote.dto

data class TokenDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val isNewUser: Boolean
)
