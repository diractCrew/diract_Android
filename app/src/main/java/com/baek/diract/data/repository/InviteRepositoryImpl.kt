package com.baek.diract.data.repository

import android.util.Log
import com.baek.diract.data.remote.api.AcceptInviteRequest
import com.baek.diract.data.remote.api.CreateInviteRequest
import com.baek.diract.data.remote.api.InviteApi
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.repository.InviteRepository
import retrofit2.HttpException
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class InviteRepositoryImpl @Inject constructor(
    private val inviteApi: InviteApi
) : InviteRepository {

    override suspend fun createInvite(teamspaceId: String): DataResult<String> = safeCall {
        val expiresAt = Instant.now().plus(24, ChronoUnit.HOURS).toString()
        val response = inviteApi.createInvite(teamspaceId, CreateInviteRequest(expiresAt))
        if (!response.success || response.data == null) throw Exception(
            response.message ?: "초대 링크 생성 실패"
        )
        "https://dancemachine-5243b.web.app/invite?token=${response.data.token}"
    }

    override suspend fun acceptInvite(token: String): DataResult<Unit> = safeCall {
        val response = inviteApi.acceptInvite(AcceptInviteRequest(token))
        if (!response.success) throw Exception(response.message ?: "초대 수락 실패")
        Unit
    }

    private inline fun <T> safeCall(block: () -> T): DataResult<T> {
        return try {
            DataResult.Success(block())
        } catch (e: Exception) {
            val serverMessage = (e as? HttpException)
                ?.response()?.errorBody()?.string()
            Log.e(TAG, "serverMessage=$serverMessage", e)
            DataResult.Error(e)
        }
    }

    companion object {
        const val TAG = "InviteRepository"
    }
}
