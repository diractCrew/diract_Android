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

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkLoginStatus()
    }

    // 앱 시작 시 토큰 있으면 바로 MainActivity로
    private fun checkLoginStatus() {
        viewModelScope.launch {
            if (!authRepository.hasToken()) return@launch
            // TODO: 테스트용 — getMe() 분기 없이 토큰만 확인. 추후 getMe() 분기 복원 필요
            _authState.value = AuthState.LoggedIn(email = "")
        }
    }

    // Google ID Token으로 서버 로그인
    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            when (val result = authRepository.loginWithGoogle(idToken)) {
                is DataResult.Success -> {
                    // TODO: 테스트용 — 무조건 회원가입 플로우로 이동. 추후 getMe() 분기 복원 필요
                    _authState.value = AuthState.NeedsSignUp
                }

                is DataResult.Error -> {
                    _authState.value = AuthState.Error(
                        message = result.throwable.message ?: "구글 로그인에 실패했습니다."
                    )
                }
            }
        }
    }

    // 로그아웃
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Idle
        }
    }

    // 구글 계정에서 가져온 이름 (UserSettingFragment 기본값)
    private val _googleDisplayName = MutableStateFlow("")
    val googleDisplayName: StateFlow<String> = _googleDisplayName.asStateFlow()

    fun setGoogleDisplayName(name: String) {
        _googleDisplayName.value = name
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
