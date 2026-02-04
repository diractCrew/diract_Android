package com.baek.diract.data.dto

import com.google.firebase.Timestamp

data class ReplyDto(
    val reply_id: String = "",
    val feedback_id: String = "",
    val author_id: String = "",
    val tagged_user_ids: List<String> = emptyList(),
    val content: String = "",
    val created_at: Timestamp? = null,
    val updated_at: Timestamp? = null
)
