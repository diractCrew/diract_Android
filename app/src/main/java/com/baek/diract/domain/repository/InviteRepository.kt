package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult

interface InviteRepository {
    // 초대 링크 생성 → 공유용 앱 링크 URL 반환
    suspend fun createInvite(teamspaceId: String): DataResult<String>

    // 초대 수락
    suspend fun acceptInvite(token: String): DataResult<Unit>
}
