package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    // 로그인 상태
    val isLoggedIn: StateFlow<Boolean>
    val currentUserInfo: StateFlow<User?>

    // TODO: 현재 로그인된 사용자 조회 (Firebase 의존 — 추후 삭제 예정)
    fun getCurrentUser(): FirebaseUser?

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
