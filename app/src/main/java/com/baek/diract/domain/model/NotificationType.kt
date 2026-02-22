package com.baek.diract.domain.model

enum class NotificationType {
    FEEDBACK,
    REPLY;

    companion object {
        fun from(value: String): NotificationType = entries.find {
            it.name.equals(value, ignoreCase = true)
        } ?: FEEDBACK
    }
}
