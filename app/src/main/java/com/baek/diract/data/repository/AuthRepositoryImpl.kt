package com.baek.diract.data.repository

import android.util.Log
import com.baek.diract.data.local.TokenManager
import com.baek.diract.data.remote.api.AuthApi
import com.baek.diract.data.remote.api.GoogleLoginRequest
import com.baek.diract.data.remote.api.UpdateMeRequest
import com.baek.diract.data.remote.api.UserApi
import com.baek.diract.data.remote.dto.toDomain
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.User
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

    private val _currentUserInfo = MutableStateFlow<User?>(null)
    override val currentUserInfo: StateFlow<User?> = _currentUserInfo.asStateFlow()

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

    override suspend fun getMe(forceRefresh: Boolean): DataResult<User> {
        // 캐시가 있고, 강제 갱신이 아니면 캐시 반환
        val cached = _currentUserInfo.value
        if (cached != null && !forceRefresh) {
            Log.d(TAG, "getMe: 캐시 반환 — $cached")
            return DataResult.Success(cached)
        }

        Log.d(TAG, "getMe: 서버 조회 시작")
        return try {
            val response = userApi.getMe()
            if (response.success && response.data != null) {
                val user = response.data.toDomain()
                Log.d(TAG, "getMe: 성공 — $user")
                _currentUserInfo.value = user
                DataResult.Success(user)
            } else {
                Log.w(TAG, "getMe: 실패 — message=${response.message}")
                DataResult.Error(Exception(response.message ?: "유저 정보를 가져올 수 없습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getMe: 예외 발생", e)
            DataResult.Error(e)
        }
    }

    override suspend fun updateMyName(name: String): DataResult<User> {
        Log.d(TAG, "updateMyName: 이름 설정 요청 — name=$name")
        return try {
            val response = userApi.updateMe(UpdateMeRequest(name))
            if (response.success && response.data != null) {
                val user = response.data.toDomain()
                Log.d(TAG, "updateMyName: 성공 — $user")
                _currentUserInfo.value = user
                DataResult.Success(user)
            } else {
                Log.w(TAG, "updateMyName: 실패 — message=${response.message}")
                DataResult.Error(Exception(response.message ?: "이름 설정에 실패했습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateMyName: 예외 발생", e)
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
                // 로그인 성공 직후 유저 정보 캐싱
                getMe(forceRefresh = true)
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

    override suspend fun deleteAccount(): DataResult<Unit> {
        Log.d(TAG, "deleteAccount: 회원 탈퇴 요청")
        return try {
            val response = userApi.deleteMe()
            if (response.success) {
                Log.d(TAG, "deleteAccount: 성공")
                logout()
                DataResult.Success(Unit)
            } else {
                Log.w(TAG, "deleteAccount: 실패 — message=${response.message}")
                DataResult.Error(Exception(response.message ?: "회원 탈퇴에 실패했습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteAccount: 예외 발생", e)
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
