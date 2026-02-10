package com.baek.diract.presentation.login

// 인증 상태
sealed interface AuthState {
    data object Idle : AuthState                        // 초기 상태 (로그인 화면)
    data object Loading : AuthState
    data class LoggedIn(val email: String) : AuthState  // 기존 유저 → MainActivity
    data object NeedsSignUp : AuthState                 // 신규 유저 → 약관 → 이름 설정
    data class Error(val message: String) : AuthState
}
