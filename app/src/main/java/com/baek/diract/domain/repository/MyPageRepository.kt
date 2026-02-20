package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult

interface MyPageRepository {

    suspend fun report(
        type: String? = null,
        reportContentType: String,
        description: String,
        reportedId: String,
        videoId: String? = null,
        feedbackId: String? = null,
        replyId: String? = null
    ): DataResult<Unit>
}
