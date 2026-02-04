package com.baek.diract.domain.model

data class CommentUser(
    val userId: String? = null, //탈퇴한 사용자
    val name: String? = null //null => 탈퇴한 사용자
)
