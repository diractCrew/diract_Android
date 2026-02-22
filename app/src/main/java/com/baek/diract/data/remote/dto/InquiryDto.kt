package com.baek.diract.data.remote.dto

data class InquiryDto(
    val inquiryId: String,
    val content: String,
    val status: String,
    val userId: String,
    val userName: String,
    val createAt: String
)
