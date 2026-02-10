package com.baek.diract.data.remote.dto

import com.google.firebase.Timestamp

data class TeamspaceDto(
    val teamspace_id: String = "",
    val owner_id: String = "",
    val teamspace_name: String = "",
    val created_at: Timestamp? = null,
    val updated_at: Timestamp? = null
)