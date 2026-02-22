package com.baek.diract.domain.model

import java.time.LocalDateTime

data class Notification(
    val notificationId: String,
    val content: String,
    val senderId: String,
    val senderName: String,
    val videoId: String? = null,
    val videoTitle: String? = null,
    val teamspaceId: String,
    val teamspaceName: String,
    val type: NotificationType,
    val isRead: Boolean,
    val createdAt: LocalDateTime
)
