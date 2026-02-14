package com.baek.diract.data.remote

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.baek.diract.DiractApplication.Companion.CHANNEL_ID
import com.baek.diract.R
import com.baek.diract.data.remote.api.EditMeRequest
import com.baek.diract.data.remote.api.UserApi
import com.baek.diract.presentation.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DiractMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userApi: UserApi

    private val serviceScope = CoroutineScope(SupervisorJob())

    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken: ${token.take(20)}...")
        serviceScope.launch {
            try {
                userApi.editMe(EditMeRequest(fcmToken = token))
                Log.d(TAG, "onNewToken: 서버 등록 성공")
            } catch (e: Exception) {
                Log.e(TAG, "onNewToken: 서버 등록 실패", e)
            }
        }
    }

    // 앱이 포그라운드일 때 호출 (백그라운드에서는 시스템이 notification 페이로드를 자동 표시)
    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "onMessageReceived: from=${message.from}")
        val notification = message.notification ?: return
        val title = notification.title ?: getString(R.string.app_name)
        val body = notification.body ?: return

        showNotification(title, body, message.data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // 서버에서 전달한 data 필드를 Intent에 포함
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val requestCode = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(requestCode, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "showNotification: 알림 권한 없음", e)
        }
    }

    companion object {
        private const val TAG = "DiractMessaging"
    }
}