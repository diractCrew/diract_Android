package com.baek.diract.data.repository

import android.util.Log
import com.baek.diract.data.remote.api.NotificationApi
import com.baek.diract.data.remote.dto.NotificationDto
import com.baek.diract.data.remote.dto.toDomain
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Notification
import com.baek.diract.domain.repository.NotificationRepository
import retrofit2.HttpException
import java.time.LocalDateTime
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationApi: NotificationApi
) : NotificationRepository {

    override suspend fun getNotifications(): DataResult<List<Notification>> = safeCall {
        val response = notificationApi.getNotifications()
        if (!response.success || response.data == null) throw Exception(
            response.message ?: "알림 목록 조회 실패"
        )
        response.data.map { it.toDomain() }
    }

    override suspend fun markNotificationAsRead(notificationId: String): DataResult<Unit> =
        safeCall {
            val response = notificationApi.markNotificationAsRead(notificationId)
            if (!response.success) throw Exception(response.message ?: "알림 읽음 처리 실패")
            Unit
        }

    private inline fun <T> safeCall(block: () -> T): DataResult<T> {
        return try {
            DataResult.Success(block())
        } catch (e: Exception) {
            val serverMessage = (e as? HttpException)
                ?.response()?.errorBody()?.string()
            Log.e(TAG, "serverMessage=$serverMessage", e)
            DataResult.Error(e)
        }
    }

    companion object {
        const val TAG = "NotificationRepository"
    }
}
