package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    // 로그인 상태 Flow (전역에서 관찰 가능)
    val isLoggedIn: StateFlow<Boolean>

    // 캐싱된 유저 정보 (getMe 호출 시 자동 갱신)
    val currentUserInfo: StateFlow<User?>

    // TODO: 현재 로그인된 사용자 조회 (Firebase 의존 — 추후 삭제 예정)
    fun getCurrentUser(): FirebaseUser?

    // Google ID Token으로 서버 로그인 (서버가 자체 JWT 발급)
    suspend fun loginWithGoogle(idToken: String): DataResult<Unit>

    // 내 정보 조회 (캐시 우선, forceRefresh로 서버 갱신)
    suspend fun getMe(forceRefresh: Boolean = false): DataResult<User>

    // 이름 설정 (회원가입 프로필 설정)
    suspend fun updateMyName(name: String): DataResult<User>

    // 로그인 여부 확인 (토큰 존재 여부)
    suspend fun hasToken(): Boolean

    // 로그아웃
    suspend fun logout()

    // 회원 탈퇴
    suspend fun deleteAccount(): DataResult<Unit>
}
