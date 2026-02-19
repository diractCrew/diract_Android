package com.baek.diract.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.R
import com.baek.diract.data.local.UserPreferenceManager
import com.baek.diract.data.remote.dto.UserDto
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.TeamMemberSummary
import com.baek.diract.domain.model.TeamspaceSummary
import com.baek.diract.domain.repository.AuthRepository
import com.baek.diract.domain.repository.TeamspaceRepository
import com.baek.diract.presentation.common.ToastEvent
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageTeamspaceViewModel @Inject constructor(
    private val teamspaceRepository: TeamspaceRepository,
    private val authRepository: AuthRepository,
    private val userPreferenceManager: UserPreferenceManager,
) : ViewModel() {
    private val _teamspaceCreatedEvent = MutableSharedFlow<Pair<String, String>>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val teamspaceCreatedEvent: SharedFlow<Pair<String, String>> = _teamspaceCreatedEvent.asSharedFlow()
    private val _transferLeaderUiState = MutableStateFlow<UiState<Long>>(UiState.None)
    val transferLeaderUiState: StateFlow<UiState<Long>> = _transferLeaderUiState.asStateFlow()

    private val _isLeader = MutableStateFlow(false)
    val isLeader: StateFlow<Boolean> = _isLeader.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<Long>>(UiState.None)
    val uiState: StateFlow<UiState<Long>> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<ToastEvent>()
    val toastMessage: SharedFlow<ToastEvent> = _toastMessage.asSharedFlow()

    private val _navEvent = MutableSharedFlow<NavEvent>()
    val navEvent: SharedFlow<NavEvent> = _navEvent.asSharedFlow()

    private val _members = MutableStateFlow<List<TeamMemberUi>>(emptyList())
    val members: StateFlow<List<TeamMemberUi>> = _members.asStateFlow()

    private val _teamspaces = MutableStateFlow<List<TeamspaceSummary>>(emptyList())
    val teamspaces: StateFlow<List<TeamspaceSummary>> = _teamspaces.asStateFlow()

    private val _createTeamspaceUiState = MutableStateFlow<UiState<Long>>(UiState.None)
    val createTeamspaceUiState: StateFlow<UiState<Long>> = _createTeamspaceUiState.asStateFlow()

    private val _renameTeamspaceUiState = MutableStateFlow<UiState<Long>>(UiState.None)
    val renameTeamspaceUiState: StateFlow<UiState<Long>> = _renameTeamspaceUiState.asStateFlow()

    fun resetCreateTeamspaceUiState() { _createTeamspaceUiState.value = UiState.None }
    fun resetRenameTeamspaceUiState() { _renameTeamspaceUiState.value = UiState.None }

    private var teamspaceId: String? = null

    fun saveLastTeamspaceId(id: String) {
        viewModelScope.launch {
            runCatching { userPreferenceManager.saveLastTeamspaceId(id) }
        }
    }
    fun setTeamspaceId(id: String) { teamspaceId = id }

    private fun requireTeamspaceId(): String =
        teamspaceId?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("TeamspaceId is not set. call setTeamspaceId() first.")

    private fun currentUserId(): String =
        authRepository.currentUserInfo.value?.userId.orEmpty()
    private suspend fun ensureUserLoaded() {
        if (!authRepository.currentUserInfo.value?.userId.isNullOrBlank()) return
        authRepository.getMe(forceRefresh = false)
    }
    fun loadMembers() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            ensureUserLoaded()
            val id = requireTeamspaceId()

            // 1) 상세에서 ownerId 얻기
            when (val detailResult = teamspaceRepository.getTeamspaceDetail(id)) {
                is DataResult.Success -> {
                    val ownerId = detailResult.data.ownerId


                    val uid = currentUserId()
                    val isOwner = (uid.isNotBlank() && uid == ownerId)


                    _isLeader.value = isOwner
                    // 2) 멤버 목록
                    when (val membersResult = teamspaceRepository.getMembers(id)) {
                        is DataResult.Success -> {
                            _members.value = membersResult.data.map { it.toUi(ownerId) }
                            _uiState.value = UiState.Success(System.currentTimeMillis())
                        }
                        is DataResult.Error -> {
                            _uiState.value = UiState.Error(
                                membersResult.throwable.message,
                                membersResult.throwable
                            )
                        }
                    }
                }
                is DataResult.Error -> {
                    _uiState.value = UiState.Error(
                        detailResult.throwable.message,
                        detailResult.throwable
                    )
                }
            }
        }
    }



    fun loadTeamspaces() {
        viewModelScope.launch {
            when (val r = teamspaceRepository.getMyTeamspaces()) {
                is DataResult.Success -> _teamspaces.value = r.data
                is DataResult.Error -> _teamspaces.value = emptyList()
            }
        }
    }

    fun createTeamspace(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            _createTeamspaceUiState.value = UiState.Loading

            when (val result = teamspaceRepository.createTeamspace(name)) {
                is DataResult.Success -> {
                    val newId = result.data.id
                    setTeamspaceId(newId)

                    loadTeamspaces()
                    loadMembers()

                    _teamspaceCreatedEvent.tryEmit(newId to name) // ✅ 여기 추가
                    _createTeamspaceUiState.value = UiState.Success(System.currentTimeMillis())
                }
                is DataResult.Error -> {
                    _createTeamspaceUiState.value = UiState.Error(
                        result.throwable.message,
                        result.throwable
                    )
                    _toastMessage.emit(ToastEvent(R.string.teamspace_create_failed, true))
                }
            }
        }
    }
    fun resetTransferLeaderUiState() { _transferLeaderUiState.value = UiState.None }
    fun renameTeamspace(newName: String) {
        if (newName.isBlank()) return

        viewModelScope.launch {
            _renameTeamspaceUiState.value = UiState.Loading

            val id = requireTeamspaceId()

            when (val result = teamspaceRepository.renameTeamspace(id, newName)) {
                is DataResult.Success -> {
                    loadTeamspaces()
                    _renameTeamspaceUiState.value = UiState.Success(System.currentTimeMillis())
                }
                is DataResult.Error -> {
                    _renameTeamspaceUiState.value = UiState.Error(result.throwable.message, result.throwable)
                    _toastMessage.emit(ToastEvent(R.string.teamspace_rename_failed, true))
                }
            }
        }
    }

    fun kickMembers(memberIds: List<String>) {
        if (memberIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val id = requireTeamspaceId()

            when (val result = teamspaceRepository.kickMembers(id, memberIds)) {
                is DataResult.Success -> {
                    _members.value = _members.value.filterNot { it.id in memberIds }
                    _uiState.value = UiState.Success(System.currentTimeMillis())
                }
                is DataResult.Error -> {
                    _uiState.value = UiState.Error(result.throwable.message, result.throwable)
                }
            }
        }
    }

    fun leaveTeamspace() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val teamId = requireTeamspaceId()

            val uid = currentUserId()
            Log.e("LeaveFlow", "uid=[$uid], len=${uid.length}")

            if (uid.isBlank()) {
                _uiState.value = UiState.Error("userId is blank (currentUserInfo is null)", null)
                _toastMessage.emit(ToastEvent(R.string.teamspace_leave_failed, true))
                return@launch
            }

            when (val result = teamspaceRepository.leaveTeamspace(teamId, uid)) {
                is DataResult.Success -> {
                    _uiState.value = UiState.Success(System.currentTimeMillis())
                    _navEvent.emit(NavEvent.Close)
                }
                is DataResult.Error -> {
                    _uiState.value = UiState.Error(result.throwable.message, result.throwable)
                    _toastMessage.emit(ToastEvent(R.string.teamspace_leave_failed, true))
                }
            }
        }}

    fun deleteTeamspace() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val id = requireTeamspaceId()

            when (val result = teamspaceRepository.deleteTeamspace(id)) {
                is DataResult.Success -> {
                    _uiState.value = UiState.Success(System.currentTimeMillis())
                    _navEvent.emit(NavEvent.Close)
                }
                is DataResult.Error -> {
                    _uiState.value = UiState.Error(result.throwable.message, result.throwable)
                }
            }
        }
    }

    fun transferLeader(newLeaderId: String) {
        if (newLeaderId.isBlank()) return

        viewModelScope.launch {
            _transferLeaderUiState.value = UiState.Loading
            val id = requireTeamspaceId()

            when (val r = teamspaceRepository.transferLeader(id, newLeaderId)) {
                is DataResult.Success -> {
                    // 리스트/리더 뱃지 갱신은 여기서 해도 되고, FragmentResult 받은 뒤 해도 됨
                    loadMembers()
                    _transferLeaderUiState.value = UiState.Success(System.currentTimeMillis())
                }
                is DataResult.Error -> {
                    _transferLeaderUiState.value = UiState.Error(r.throwable.message, r.throwable)
                }
            }
        }
    }
}

sealed interface NavEvent {
    data object Close : NavEvent
}

private fun TeamMemberSummary.toUi(ownerId: String): TeamMemberUi =
    TeamMemberUi(
        id = this.id,
        name = this.name,
        isLeader = (this.id == ownerId)
    )
