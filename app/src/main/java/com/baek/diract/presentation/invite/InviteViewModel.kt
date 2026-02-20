package com.baek.diract.presentation.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.repository.InviteRepository
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InviteViewModel @Inject constructor(
    private val inviteRepository: InviteRepository
) : ViewModel() {

    // MainActivity → HomeFragment로 토큰을 전달하는 채널 (null = 없음)
    private val _pendingToken = MutableStateFlow<String?>(null)
    val pendingToken: StateFlow<String?> = _pendingToken.asStateFlow()

    private val _acceptUiState = MutableStateFlow<UiState<Unit>>(UiState.None)
    val acceptUiState: StateFlow<UiState<Unit>> = _acceptUiState.asStateFlow()

    fun setPendingToken(token: String) {
        _pendingToken.value = token
    }

    fun consumeToken() {
        _pendingToken.value = null
    }

    fun acceptInvite(token: String) {
        viewModelScope.launch {
            _acceptUiState.value = UiState.Loading
            when (val result = inviteRepository.acceptInvite(token)) {
                is DataResult.Success -> _acceptUiState.value = UiState.Success(result.data)
                is DataResult.Error -> _acceptUiState.value = UiState.Error(
                    result.throwable.message,
                    result.throwable
                )
            }
        }
    }

    fun resetAcceptUiState() {
        _acceptUiState.value = UiState.None
    }
}