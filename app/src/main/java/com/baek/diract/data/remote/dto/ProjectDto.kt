package com.baek.diract.data.remote.dto

import com.google.firebase.Timestamp

//TODO: 삭제 or 수정
data class ProjectDto(
    val project_id: String = "",
    val teamspace_id: String = "",
    val creator_id: String = "",
    val project_name: String = "",
    val created_at: Timestamp? = null,
    val updated_at: Timestamp? = null
)
