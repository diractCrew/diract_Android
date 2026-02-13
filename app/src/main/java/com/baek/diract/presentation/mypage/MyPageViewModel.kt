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

    //로그인/탈퇴 로딩 상태
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastEvent = MutableSharedFlow<ToastEvent>()
    val toastEvent: SharedFlow<ToastEvent> = _toastEvent.asSharedFlow()

    // 로그아웃/탈퇴 성공 시 LoginActivity로 이동 이벤트
    private val _navigateToLogin = MutableSharedFlow<Unit>()
    val navigateToLogin: SharedFlow<Unit> = _navigateToLogin.asSharedFlow()

    // 변경 성공 시 뒤로가기 이벤트
    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    init {
        getUserInfo()
    }

    private fun getUserInfo() {
        viewModelScope.launch {
            authRepository.getMe()
        }
    }

    /*
        EditUserNameFragment 로직
     */

    // 이름 변경 로딩 상태
    private val _isUpdatingName = MutableStateFlow(false)
    val isUpdatingName: StateFlow<Boolean> = _isUpdatingName.asStateFlow()

    fun updateMyName(name: String) {
        viewModelScope.launch {
            _isUpdatingName.value = true
            when (authRepository.updateMyName(name)) {
                is DataResult.Success -> {
                    _isUpdatingName.value = false
                    _navigateBack.emit(Unit)
                }

                is DataResult.Error -> {
                    _isUpdatingName.value = false
                    _toastEvent.emit(ToastEvent(R.string.update_name_failed, isErr = true))
                }
            }
        }
    }

    /*
        InquiryFragment 로직
     */
    private val _isSubmittingInquiry = MutableStateFlow(false)
    val isSubmittingInquiry: StateFlow<Boolean> = _isSubmittingInquiry.asStateFlow()

    fun submitInquiry(content: String) {
        viewModelScope.launch {
            _isSubmittingInquiry.value = true
            // TODO: 문의 접수 API 호출
            /*
                TODO: 문의하기 api 호출
                - 성공/실패 시 토스트 emit하기
             */
            _isSubmittingInquiry.value = false
            _navigateBack.emit(Unit)
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
