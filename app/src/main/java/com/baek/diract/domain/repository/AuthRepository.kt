package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    // 로그인 상태
    val isLoggedIn: StateFlow<Boolean>
    val currentUserInfo: StateFlow<User?>

    //  인증
    suspend fun hasToken(): Boolean
    suspend fun loginWithGoogle(idToken: String): DataResult<Boolean>

    //  회원가입
    suspend fun savePendingTokens()
    suspend fun agreeTerms(): DataResult<User>
    suspend fun updateMyName(name: String): DataResult<User>

    // 유저 정보
    suspend fun getMe(forceRefresh: Boolean = false): DataResult<User>
    suspend fun registerFcmToken(): DataResult<User>

    // 로그아웃 / 탈퇴
    suspend fun logout()
    suspend fun deleteAccount(): DataResult<Unit>
}
