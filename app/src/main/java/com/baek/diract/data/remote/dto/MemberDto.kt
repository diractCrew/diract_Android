package com.baek.diract.data.remote.dto

import com.google.firebase.Timestamp

//TODO: 삭제 or 수정
data class MembersDto(
    val user_id: String = "",
    val joined_at: Timestamp? = null
)
