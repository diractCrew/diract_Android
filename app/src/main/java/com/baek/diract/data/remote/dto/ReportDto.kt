package com.baek.diract.data.remote.dto

data class ReportDto(
    val reportId: String,
    val type: String,
    val reportContentType: String,
    val description: String,
    val reporterId: String,
    val reportedId: String,
    val status: String,
    val videoId: String? = null,
    val feedbackId: String? = null,
    val replyId: String? = null,
    val createdAt: String,
    val updatedAt: String
)
