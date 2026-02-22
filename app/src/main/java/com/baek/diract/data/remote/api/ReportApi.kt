package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.ReportDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportApi {

    @POST("api/reports")
    suspend fun createReport(
        @Body request: CreateReportRequest
    ): ApiResponse<ReportDto>
}

data class CreateReportRequest(
    val type: String,
    val reportContentType: String,
    val description: String,
    val reportedId: String,
    val videoId: String? = null,
    val feedbackId: String? = null,
    val replyId: String? = null
)
