package com.baek.diract.presentation.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Notification
import com.baek.diract.domain.repository.NotificationRepository
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Notification>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Notification>>> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = when (val result = notificationRepository.getNotifications()) {
                is DataResult.Success -> UiState.Success(result.data)
                is DataResult.Error -> UiState.Error(
                    message = result.throwable.message,
                    throwable = result.throwable
                )
            }
        }
    }

    fun markAsRead(notificationId: String) {
        // 로컬 상태 즉시 업데이트 → DiffCallback이 해당 아이템만 리바인드
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(
            current.map { if (it.notificationId == notificationId) it.copy(isRead = true) else it }
        )
        viewModelScope.launch {
            notificationRepository.markNotificationAsRead(notificationId)
        }
    }
}