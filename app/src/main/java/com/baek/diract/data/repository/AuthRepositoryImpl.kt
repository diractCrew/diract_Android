package com.baek.diract.data.repository

import android.util.Log
import com.baek.diract.data.local.TokenManager
import com.baek.diract.data.remote.api.AuthApi
import com.baek.diract.data.remote.api.GoogleLoginRequest
import com.baek.diract.data.remote.api.UserApi
import com.baek.diract.data.remote.dto.UserDto
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val tokenManager: TokenManager,
    // Firebase 의존 — 추후 삭제 예정 (다른 ViewModel에서 getCurrentUser 사용 중)
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserInfo = MutableStateFlow<UserDto?>(null)
    override val currentUserInfo: StateFlow<UserDto?> = _currentUserInfo.asStateFlow()

    // 초기 로그인 상태를 TokenManager에서 확인
    suspend fun checkInitialLoginState() {
        _isLoggedIn.value = tokenManager.accessToken.first() != null
    }

    // Firebase 의존 — 추후 삭제 예정 (다른 ViewModel에서 사용 중)
    override fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    override suspend fun hasToken(): Boolean {
        val has = tokenManager.accessToken.first() != null
        Log.d(TAG, "hasToken: $has")
        return has
    }

    override suspend fun getMe(): DataResult<UserDto> {
        Log.d(TAG, "getMe: 유저 정보 조회 시작")
        return try {
            val response = userApi.getMe()
            if (response.success && response.data != null) {
                Log.d(TAG, "getMe: 성공 — ${response.data}")
                _currentUserInfo.value = response.data
                DataResult.Success(response.data)
            } else {
                Log.w(TAG, "getMe: 실패 — message=${response.message}")
                DataResult.Error(Exception(response.message ?: "유저 정보를 가져올 수 없습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getMe: 예외 발생", e)
            DataResult.Error(e)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): DataResult<Unit> {
        Log.d(TAG, "loginWithGoogle: 서버 로그인 요청 — idToken=${idToken.take(20)}...")
        return try {
            val response = authApi.loginWithGoogle(GoogleLoginRequest(idToken))
            if (response.success && response.data != null) {
                Log.d(TAG, "loginWithGoogle: 성공 — 토큰 저장")
                tokenManager.saveTokens(
                    accessToken = response.data.accessToken,
                    refreshToken = response.data.refreshToken
                )
                _isLoggedIn.value = true
                DataResult.Success(Unit)
            } else {
                Log.w(TAG, "loginWithGoogle: 실패 — message=${response.message}")
                DataResult.Error(Exception(response.message ?: "로그인에 실패했습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "loginWithGoogle: 예외 발생", e)
            DataResult.Error(e)
        }
    }

    override suspend fun logout() {
        Log.d(TAG, "logout: 토큰 삭제")
        tokenManager.clearTokens()
        _currentUserInfo.value = null
        _isLoggedIn.value = false
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
