package com.baek.diract.data.remote.dto

import com.baek.diract.domain.model.User

data class UserDto(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val loginType: String = "",
    val status: String = "",
    val lastLoginAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

fun UserDto.toDomain() = User(
    userId = userId,
    email = email,
    name = name
)
