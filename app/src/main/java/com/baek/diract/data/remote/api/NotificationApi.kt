package com.baek.diract.data.remote.api

import com.baek.diract.data.remote.dto.ApiResponse
import com.baek.diract.data.remote.dto.NotificationDto
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface NotificationApi {

    @GET("api/notifications")
    suspend fun getNotifications(): ApiResponse<List<NotificationDto>>

    @PATCH("api/notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(
        @Path("notificationId") notificationId: String,
    ): ApiResponse<Unit>
}
