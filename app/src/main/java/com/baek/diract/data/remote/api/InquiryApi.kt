package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.InquiryDto
import retrofit2.http.Body
import retrofit2.http.POST

interface InquiryApi {

    @POST("api/inquiries")
    suspend fun createInquiry(
        @Body request: CreateInquiryRequest
    ): ApiResponse<InquiryDto>
}

data class CreateInquiryRequest(
    val content: String
)
