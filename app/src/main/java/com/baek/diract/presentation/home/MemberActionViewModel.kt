package com.baek.diract.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.repository.TeamspaceRepository
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LeaderUiState {
    data object Idle : LeaderUiState
    data object Loading : LeaderUiState
    data object Success : LeaderUiState
    data class Error(val message: String?) : LeaderUiState
}

@HiltViewModel
class MemberActionViewModel @Inject constructor(
    private val teamspaceRepository: TeamspaceRepository
) : ViewModel() {

    private val _leaderState = MutableStateFlow<LeaderUiState>(LeaderUiState.Idle)
    val leaderState: StateFlow<LeaderUiState> = _leaderState.asStateFlow()

    fun reset() { _leaderState.value = LeaderUiState.Idle }

    fun transferLeader(teamspaceId: String, newLeaderId: String) {
        viewModelScope.launch {
            _leaderState.value = LeaderUiState.Loading
            when (val r = teamspaceRepository.transferLeader(teamspaceId, newLeaderId)) {
                is DataResult.Success -> _leaderState.value = LeaderUiState.Success
                is DataResult.Error -> _leaderState.value = LeaderUiState.Error(r.throwable.message)
            }
        }
    }
}
