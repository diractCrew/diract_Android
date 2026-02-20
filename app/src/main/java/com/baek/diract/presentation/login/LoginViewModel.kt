package com.baek.diract.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUserInfo = authRepository.currentUserInfo

    init {
        checkLoginStatus()
    }

    // 앱 시작 시 토큰 확인 후 분기
    private fun checkLoginStatus() {
        viewModelScope.launch {
            if (authRepository.hasToken()) {
                authRepository.getMe()
                _authState.value = AuthState.LoggedIn(email = "")
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }

    // Google ID Token으로 서버 로그인
    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            when (val result = authRepository.loginWithGoogle(idToken)) {
                is DataResult.Success -> {
                    val isNewUser = result.data
                    _authState.value = if (isNewUser) {
                        AuthState.NeedsSignUp
                    } else {
                        AuthState.LoggedIn(email = "")
                    }
                }

                is DataResult.Error -> {
                    _authState.value = AuthState.Error(
                        message = result.throwable.message ?: "구글 로그인에 실패했습니다."
                    )
                }
            }
        }
    }

    // 회원가입 완료 (토큰 저장 + 약관 동의 전송)
    fun completeSignUp() {
        viewModelScope.launch {
            authRepository.savePendingTokens()
            authRepository.agreeTerms()
        }
    }

    // 이름 설정 (회원가입 마지막 단계)
    private val _isProfileSaving = MutableStateFlow(false)
    val isProfileSaving: StateFlow<Boolean> = _isProfileSaving.asStateFlow()

    fun updateMyName(name: String) {
        viewModelScope.launch {
            _isProfileSaving.value = true
            when (val result = authRepository.updateMyName(name)) {
                is DataResult.Success -> {
                    _authState.value = AuthState.LoggedIn(email = result.data.email)
                }

                is DataResult.Error -> {
                    _authState.value = AuthState.Error(
                        message = result.throwable.message ?: "이름 설정에 실패했습니다."
                    )
                }
            }
            _isProfileSaving.value = false
        }
    }

    // 약관 동의 상태
    private val _isPrivacyAgreed = MutableStateFlow(false)
    val isPrivacyAgreed: StateFlow<Boolean> = _isPrivacyAgreed.asStateFlow()

    private val _isServiceAgreed = MutableStateFlow(false)
    val isServiceAgreed: StateFlow<Boolean> = _isServiceAgreed.asStateFlow()

    private val _isAgeAgreed = MutableStateFlow(false)
    val isAgeAgreed: StateFlow<Boolean> = _isAgeAgreed.asStateFlow()

    fun togglePrivacyAgreed() {
        _isPrivacyAgreed.value = !_isPrivacyAgreed.value
    }

    fun toggleServiceAgreed() {
        _isServiceAgreed.value = !_isServiceAgreed.value
    }

    fun toggleAgeAgreed() {
        _isAgeAgreed.value = !_isAgeAgreed.value
    }

    fun toggleAllAgreed() {
        val newState = !(_isPrivacyAgreed.value && _isServiceAgreed.value && _isAgeAgreed.value)
        _isPrivacyAgreed.value = newState
        _isServiceAgreed.value = newState
        _isAgeAgreed.value = newState
    }
}
