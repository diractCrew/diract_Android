package com.baek.diract.data.remote.dto

import com.google.firebase.Timestamp

//TODO: 삭제 or 수정
data class TrackDto(
    val track_id: String = "",
    val video_id: String = "",
    val section_id: String = "",
    val created_at: Timestamp? = null,
    val updated_at: Timestamp? = null
)
