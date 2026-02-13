package com.baek.diract.presentation.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.R
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.User
import com.baek.diract.domain.repository.AuthRepository
import com.baek.diract.presentation.common.ToastEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val userInfo: StateFlow<User?> = authRepository.currentUserInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastEvent = MutableSharedFlow<ToastEvent>()
    val toastEvent: SharedFlow<ToastEvent> = _toastEvent.asSharedFlow()

    // 로그아웃/탈퇴 성공 시 LoginActivity로 이동 이벤트
    private val _navigateToLogin = MutableSharedFlow<Unit>()
    val navigateToLogin: SharedFlow<Unit> = _navigateToLogin.asSharedFlow()

    init {
        getUserInfo()
    }

    private fun getUserInfo() {
        viewModelScope.launch {
            authRepository.getMe()
        }
    }


    /*
        AccountSettingFragment 로직
     */
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.logout()
            _isLoading.value = false
            _navigateToLogin.emit(Unit)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = authRepository.deleteAccount()) {
                is DataResult.Success -> {
                    _isLoading.value = false
                    _navigateToLogin.emit(Unit)
                }
                is DataResult.Error -> {
                    _isLoading.value = false
                    _toastEvent.emit(ToastEvent(R.string.delete_account_failed, isErr = true))
                }
            }
        }
    }
}
