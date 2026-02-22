package com.baek.diract.data.remote.dto

import com.baek.diract.data.mapper.parseDateTime
import com.baek.diract.domain.model.Notification
import com.baek.diract.domain.model.NotificationType
import java.time.LocalDateTime

data class NotificationDto(
    val notificationId: String,
    val content: String,
    val senderId: String,
    val senderName: String,
    val videoId: String,
    val videoTitle: String,
    val teamspaceId: String,
    val teamspaceName: String,
    val type: String,
    val isRead: String,
    val createdAt: String
)

fun NotificationDto.toDomain() = Notification(
    notificationId = notificationId,
    content = content,
    senderId = senderId,
    senderName = senderName,
    videoId = videoId,
    videoTitle = videoTitle,
    teamspaceId = teamspaceId,
    teamspaceName = teamspaceName,
    type = NotificationType.from(type),
    isRead = isRead.toBoolean(),
    createdAt = parseDateTime(createdAt)
)
