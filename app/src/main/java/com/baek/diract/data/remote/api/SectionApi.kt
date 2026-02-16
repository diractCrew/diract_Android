package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.SectionCreateDto
import com.baek.diract.data.remote.dto.SectionDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface SectionApi {

    //섹션(Section) 생성 API
    @POST("api/tracks/{tracksId}/sections")
    suspend fun createSection(
        @Path("tracksId") tracksId: String,
        @Body request: CreateSectionRequest
    ): ApiResponse<SectionCreateDto>

    //섹션(Section) 목록 조회 API
    @GET("api/tracks/{tracksId}/sections")
    suspend fun getSectionList(
        @Path("tracksId") tracksId: String
    ): ApiResponse<SectionDto>

    //섹션(Section) 수정 API
    @PATCH("api/tracks/{tracksId}/sections/{sectionId}")
    suspend fun editSection(
        @Path("tracksId") tracksId: String,
        @Path("sectionId") sectionId: String,
        @Body request: EditSectionRequest
    ): ApiResponse<SectionCreateDto>

    //섹션(Section) 삭제 API
    @DELETE("api/tracks/{tracksId}/sections/{sectionId}")
    suspend fun deleteSection(
        @Path("tracksId") tracksId: String,
        @Path("sectionId") sectionId: String,
    ): ApiResponse<Unit>
}

data class CreateSectionRequest(
    val sectionName: String
)

data class EditSectionRequest(
    val sectionName: String
)
