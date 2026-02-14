package com.baek.diract.domain.model
data class MemberSummary(
    val userId: String,
    val name: String,
    val email: String,
    val joinedAtEpochSeconds: Long? // Timestamp 처리 편의용
)