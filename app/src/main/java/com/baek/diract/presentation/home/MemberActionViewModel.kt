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

@HiltViewModel
class MemberActionViewModel @Inject constructor(
    private val teamspaceRepository: TeamspaceRepository
) : ViewModel() {

    private val _transferState = MutableStateFlow<UiState<Long>>(UiState.None)
    val transferState: StateFlow<UiState<Long>> = _transferState.asStateFlow()

    fun transfer(teamspaceId: String, newOwnerId: String) {
        viewModelScope.launch {
            _transferState.value = UiState.Loading
            when (val r = teamspaceRepository.transferLeader(teamspaceId, newOwnerId)) {
                is DataResult.Success -> _transferState.value = UiState.Success(System.currentTimeMillis())
                is DataResult.Error -> _transferState.value = UiState.Error(r.throwable.message, r.throwable)
            }
        }
    }

    fun reset() { _transferState.value = UiState.None }
}
