package com.baek.diract.data.dto

import com.google.firebase.Timestamp

data class FeedbackDto(
    val feedback_id: String = "",
    val video_id: String = "",
    val author_id: String? = null, //탈퇴한 사용자?
    val tagged_user_ids: List<String> = emptyList(),
    val content: String = "",
    val created_at: Timestamp? = null,
    val updated_at: Timestamp? = null,
    val start_time: Double? = null,
    val end_time: Double? = null,
    val teamspace_id: String = "",
    val image_url: String? = null
)
