package com.baek.diract.data.dto

import com.google.firebase.Timestamp

data class UsersDto(
    val user_id: String = "",
    val email: String = "",
    val name: String = "",
    val login_type: String = "",
    val status: String = "active",
    val fcm_token: String = "",
    val terms_agreed: Boolean = false,
    val privacy_agreed: Boolean = false,
    val created_at: Timestamp? = null,
    val updated_at: Timestamp? = null,
    val last_login_at: Timestamp? = null
)
