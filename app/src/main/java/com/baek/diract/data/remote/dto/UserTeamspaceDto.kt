package com.baek.diract.data.remote.dto

import java.sql.Timestamp

//TODO: 삭제 or 수정
data class UserTeamspaceDto(
    val teamspace_id: String = "",
    val joined_at: Timestamp? = null
)
