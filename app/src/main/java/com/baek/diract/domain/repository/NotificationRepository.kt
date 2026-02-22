package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Notification

interface NotificationRepository {
    suspend fun getNotifications(): DataResult<List<Notification>>
    suspend fun markNotificationAsRead(notificationId: String): DataResult<Unit>
}
