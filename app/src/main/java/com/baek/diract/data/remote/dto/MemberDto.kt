package com.baek.diract.data.remote.dto

import com.google.firebase.Timestamp

data class MemberDto(
    val user_id: String = "",
    val user_name: String = "",
    val joined_at: Timestamp? = null
)