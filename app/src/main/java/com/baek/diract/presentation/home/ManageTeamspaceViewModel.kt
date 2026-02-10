package com.baek.diract.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.R
import com.baek.diract.domain.repository.TeamspaceRepository
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 팀스페이스 관리 화면 전용 VM
 * - 로딩/성공/에러 상태 (오버레이에 연결)
 * - 토스트/네비 같은 1회성 이벤트
 * - 멤버 내보내기, 팀스페이스 나가기, 삭제 같은 액션 실행
 */
@HiltViewModel
class ManageTeamspaceViewModel @Inject constructor(
    // TODO: 여기 너희 레포/유스케이스 주입
    private val teamspaceRepository: TeamspaceRepository,
) : ViewModel() {

    // ---- 화면 전용 실행 상태(오버레이) ----
    private val _state = MutableStateFlow<UiState<ManageResult>>(UiState.None)
    val state = _state.asStateFlow()

    // ---- 1회성 토스트(메시지 리소스 id) ----
    private val _toast = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val toast = _toast.asSharedFlow()

    // ---- 1회성 네비 이벤트(선택) ----
    private val _nav = MutableSharedFlow<NavEvent>(extraBufferCapacity = 1)
    val nav = _nav.asSharedFlow()

    // ---- (선택) 화면 데이터: 멤버 목록 등 ----
    private val _members = MutableStateFlow<List<TeamMemberUi>>(emptyList())
    val members = _members.asStateFlow()
    private var currentTeamspaceId: Long? = null

    fun setTeamspaceId(teamspaceId: Long) {
        currentTeamspaceId = teamspaceId
    }

    fun createTeamspace(name: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                // TODO repo.createTeamspace(name)
            }.onSuccess {
                _state.value = UiState.Success(data = ManageResult.CreatedTeamspace)
            }.onFailure { e ->
                _state.value = UiState.Error(message = e.message, throwable = e)
                _toast.tryEmit(R.string.error_create_teamspace)
            }
        }
    }

    // ============ 로드/갱신 ============
    fun loadMembers(teamspaceId: Long = requireTeamspaceId()) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                // TODO repo.getMembers(teamspaceId) -> List<TeamMemberUi>
                emptyList<TeamMemberUi>()
            }.onSuccess { list ->
                _members.value = list
                _state.value = UiState.Success(data = ManageResult.Refreshed)
            }.onFailure { e ->
                _state.value = UiState.Error(message = e.message, throwable = e)
                _toast.tryEmit(R.string.error_load_members) // 필요하면 추가
            }
        }
    }

    // ============ 액션들 ============
    fun kickMembers(memberIds: List<String>) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                // TODO repo.kickMembers(teamspaceId, memberIds)
            }.onSuccess {
                _state.value = UiState.Success(data = ManageResult.Kicked(memberIds))
                _members.value = _members.value.filterNot { it.id in memberIds }
            }.onFailure { e ->
                _state.value = UiState.Error(message = e.message, throwable = e)
                _toast.tryEmit(R.string.error_kick_members) // 없으면 추가
            }
        }
    }

    fun leaveTeamspace(teamspaceId: Long = requireTeamspaceId()) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                // TODO: teamspaceRepository.leaveTeamspace(teamspaceId)
            }.onSuccess {
                _state.value = UiState.Success(ManageResult.LeftTeamspace(teamspaceId))
                // 성공 시 화면 닫기 같은 네비 이벤트
                _nav.tryEmit(NavEvent.Close)
            }.onFailure { e ->
                _state.value = UiState.Error(message = e.message, throwable = e)
                // _toast.tryEmit(R.string.leave_fail)
            }
        }
    }

    fun deleteTeamspace(teamspaceId: Long = requireTeamspaceId()) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching {
                // TODO: teamspaceRepository.deleteTeamspace(teamspaceId)
            }.onSuccess {
                _state.value = UiState.Success(ManageResult.DeletedTeamspace(teamspaceId))
                _nav.tryEmit(NavEvent.Close)
            }.onFailure { e ->
                _state.value = UiState.Error(message = e.message, throwable = e)
                // _toast.tryEmit(R.string.delete_fail)
            }
        }
    }

    // ---- 유틸 ----
    private fun requireTeamspaceId(): Long =
        currentTeamspaceId ?: error("TeamspaceId is not set. call setTeamspaceId() first.")
}

/** 성공 시 어떤 액션이 완료됐는지 구분용 */
sealed interface ManageResult {
    data object Refreshed : ManageResult
    data object CreatedTeamspace : ManageResult
    data class LeftTeamspace(val teamspaceId: Long) : ManageResult
    data class DeletedTeamspace(val teamspaceId: Long) : ManageResult
    data class Kicked(val memberIds: List<String>) : ManageResult
}

/** 화면 이동 같은 1회성 이벤트 */
sealed interface NavEvent {
    data object Close : NavEvent
}

/** 멤버 UI 모델(네 도메인 모델에 맞게 바꿔) */
data class MemberUi(
    val id: Long,
    val name: String,
    val role: String,
)
