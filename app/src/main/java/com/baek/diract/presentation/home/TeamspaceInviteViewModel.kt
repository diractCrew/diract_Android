package com.baek.diract.presentation.home

import androidx.lifecycle.SavedStateHandle
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
class TeamspaceInviteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val inviteRepository: InviteRepository
) : ViewModel() {

    private val teamspaceId: String = checkNotNull(savedStateHandle["teamspaceId"]) {
        "teamspaceId 없이 TeamspaceInvite 접근이 불가능합니다."
    }

    // 초대 링크 URL을 data로 전달
    private val _inviteState = MutableStateFlow<UiState<String>>(UiState.None)
    val inviteState: StateFlow<UiState<String>> = _inviteState.asStateFlow()

    fun createInviteLink() {
        viewModelScope.launch {
            _inviteState.value = UiState.Loading
            _inviteState.value = when (val result = inviteRepository.createInvite(teamspaceId)) {
                is DataResult.Success -> UiState.Success(result.data)
                is DataResult.Error -> UiState.Error(
                    message = result.throwable.message,
                    throwable = result.throwable
                )
            }
        }
    }
}