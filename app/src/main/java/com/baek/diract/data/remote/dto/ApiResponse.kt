package com.baek.diract.data.remote.dto

// 서버 공통 응답 래퍼
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val timestamp: String?
)
