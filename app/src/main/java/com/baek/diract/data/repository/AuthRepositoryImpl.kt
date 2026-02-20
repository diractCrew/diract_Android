package com.baek.diract.data.repository

import android.util.Log
import com.baek.diract.data.local.TokenManager
import com.baek.diract.data.remote.api.AuthApi
import com.baek.diract.data.remote.api.EditMeRequest
import com.baek.diract.data.remote.api.GoogleLoginRequest
import com.baek.diract.data.remote.api.UserApi
import com.baek.diract.data.remote.dto.toDomain
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.User
import com.baek.diract.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val tokenManager: TokenManager,
    private val firebaseMessaging: FirebaseMessaging
) : AuthRepository {

    // 로그인 상태
    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserInfo = MutableStateFlow<User?>(null)
    override val currentUserInfo: StateFlow<User?> = _currentUserInfo.asStateFlow()

    // 인증

    // 신규 유저의 토큰을 약관 동의 전까지 메모리에 보관
    private var pendingAccessToken: String? = null
    private var pendingRefreshToken: String? = null

    override suspend fun hasToken(): Boolean {
        val has = tokenManager.accessToken.first() != null
        Log.d(TAG, "hasToken: $has")
        return has
    }

    override suspend fun loginWithGoogle(idToken: String): DataResult<Boolean> {
        Log.d(TAG, "loginWithGoogle: 서버 로그인 요청 — idToken=${idToken.take(20)}...")
        return try {
            val response = authApi.loginWithGoogle(GoogleLoginRequest(idToken))
            if (response.success && response.data != null) {
                val isNewUser = response.data.isNewUser
                Log.d(TAG, "loginWithGoogle: 성공 — isNewUser=$isNewUser")

                if (isNewUser) {
                    // 신규 유저: 약관 동의 전까지 토큰 보류
                    pendingAccessToken = response.data.accessToken
                    pendingRefreshToken = response.data.refreshToken
                } else {
                    // 기존 유저: 토큰 즉시 저장
                    tokenManager.saveTokens(
                        accessToken = response.data.accessToken,
                        refreshToken = response.data.refreshToken
                    )
                    _isLoggedIn.value = true
                    getMe(forceRefresh = true)
                    registerFcmToken()
                }

                DataResult.Success(isNewUser)
            } else {
                Log.w(TAG, "loginWithGoogle: 실패 — message=${response.message}")
                DataResult.Error(Exception(response.message ?: "로그인에 실패했습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "loginWithGoogle: 예외 발생", e)
            DataResult.Error(e)
        }
    }

    // 회원가입

    override suspend fun savePendingTokens() {
        val accessToken = pendingAccessToken ?: return
        val refreshToken = pendingRefreshToken ?: return
        Log.d(TAG, "savePendingTokens: 보류 중인 토큰 저장")
        tokenManager.saveTokens(accessToken, refreshToken)
        _isLoggedIn.value = true
        pendingAccessToken = null
        pendingRefreshToken = null
        getMe(forceRefresh = true)
        registerFcmToken()
    }

    override suspend fun agreeTerms(): DataResult<User> {
        Log.d(TAG, "agreeTerms: 약관 동의 전송")
        return try {
            val response = userApi.editMe(
                EditMeRequest(termsAgreed = true, privacyAgreed = true)
            )
            if (response.success && response.data != null) {
                val user = response.data.toDomain()
                Log.d(TAG, "agreeTerms: 성공 — ${user.name}: ${user.userId.take(8)}..")
                _currentUserInfo.value = user
                DataResult.Success(user)
            } else {
                Log.w(TAG, "agreeTerms: 실패 — message=${response.message}")
                DataResult.Error(Exception(response.message ?: "약관 동의에 실패했습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "agreeTerms: 예외 발생", e)
            DataResult.Error(e)
        }
    }

    override suspend fun updateMyName(name: String): DataResult<User> {
        Log.d(TAG, "updateMyName: 이름 설정 요청 — name=$name")
        return try {
            val response = userApi.editMe(EditMeRequest(name = name))
            if (response.success && response.data != null) {
                val user = response.data.toDomain()
                Log.d(TAG, "updateMyName: 성공 — ${user.name}: ${user.userId.take(8)}..")
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

    // 유저 정보

    override suspend fun getMe(forceRefresh: Boolean): DataResult<User> {
        val cached = _currentUserInfo.value
        if (cached != null && !forceRefresh) {
            Log.d(TAG, "getMe: 캐시 반환 — ${cached.name}: ${cached.userId.take(8)}..")
            return DataResult.Success(cached)
        }

        Log.d(TAG, "getMe: 서버 조회 시작")
        return try {
            val response = userApi.getMe()
            if (response.success && response.data != null) {
                val user = response.data.toDomain()
                Log.d(TAG, "getMe: 성공 — ${user.name}: ${user.userId.take(8)}..")
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

    override suspend fun registerFcmToken(): DataResult<User> {
        Log.d(TAG, "registerFcmToken: FCM 토큰 발급 및 서버 등록")
        return try {
            val token = firebaseMessaging.token.await()
            Log.d(TAG, "registerFcmToken: 토큰 발급 성공 — ${token.take(20)}...")
            val response = userApi.editMe(EditMeRequest(fcmToken = token))
            if (response.success && response.data != null) {
                val user = response.data.toDomain()
                Log.d(TAG, "registerFcmToken: 서버 등록 성공")
                _currentUserInfo.value = user
                DataResult.Success(user)
            } else {
                Log.w(TAG, "registerFcmToken: 서버 등록 실패 — message=${response.message}")
                DataResult.Error(Exception(response.message ?: "FCM 토큰 등록에 실패했습니다."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerFcmToken: 예외 발생", e)
            DataResult.Error(e)
        }
    }

    // 로그아웃 / 탈퇴

    override suspend fun logout() {
        Log.d(TAG, "logout: 토큰 삭제")
        tokenManager.clearTokens()
        _currentUserInfo.value = null
        _isLoggedIn.value = false
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

    companion object {
        private const val TAG = "AuthRepository"
    }
}
