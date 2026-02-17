package com.baek.diract.presentation.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.ProjectSummary
import com.baek.diract.domain.model.TeamspaceSummary
import com.baek.diract.domain.model.TracksSummary
import com.baek.diract.domain.repository.ProjectRepository
import com.baek.diract.domain.repository.TeamspaceRepository
import com.baek.diract.domain.repository.TracksRepository
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val app: Application,
    private val projectRepository: ProjectRepository,
    private val teamspaceRepository: TeamspaceRepository,
    private val tracksRepository: TracksRepository,
) : ViewModel() {

    private val _teamspaces = MutableStateFlow<List<TeamspaceSummary>>(emptyList())
    val teamspaces: StateFlow<List<TeamspaceSummary>> = _teamspaces.asStateFlow()

    // 홈 화면 전체 상태
    private val _homeUiState = MutableStateFlow<UiState<HomeUiModel>>(UiState.Loading)
    val homeUiState: StateFlow<UiState<HomeUiModel>> = _homeUiState.asStateFlow()

    private val prefs = context.getSharedPreferences("home_tips", Context.MODE_PRIVATE)

    private companion object {
        const val KEY_PROJECT_TIP_STEP = "project_tip_step"
        const val KEY_EMPTY_PROJECT_TIP_DONE = "empty_project_done"
        const val KEY_NO_TEAMSPACE_TIP_DONE = "no_teamspace_done"
    }

    private val _currentTeamspaceName = MutableStateFlow("")
    val currentTeamspaceName: StateFlow<String> = _currentTeamspaceName.asStateFlow()

    // "팀스페이스는 있는데 프로젝트 없음" 화면 툴팁 step (0,1,2)
    val projectTipStep = MutableLiveData(prefs.getInt(KEY_PROJECT_TIP_STEP, 2))

    fun setProjectTipStep(step: Int) {
        prefs.edit().putInt(KEY_PROJECT_TIP_STEP, step).apply()
        projectTipStep.value = step
    }

    fun isEmptyProjectTipDone(): Boolean =
        prefs.getBoolean(KEY_EMPTY_PROJECT_TIP_DONE, false)

    fun markEmptyProjectTipDone() {
        prefs.edit().putBoolean(KEY_EMPTY_PROJECT_TIP_DONE, true).apply()
    }

    fun isNoTeamspaceTipDone(): Boolean =
        prefs.getBoolean(KEY_NO_TEAMSPACE_TIP_DONE, false)

    fun markNoTeamspaceTipDone() {
        prefs.edit().putBoolean(KEY_NO_TEAMSPACE_TIP_DONE, true).apply()
    }

    // =========================
    // Dialog UiState
    // =========================
    private val _createTeamspaceUiState = MutableStateFlow<UiState<Long>>(UiState.None)
    val createTeamspaceUiState: StateFlow<UiState<Long>> = _createTeamspaceUiState.asStateFlow()

    private val _createProjectUiState = MutableStateFlow<UiState<Long>>(UiState.None)
    val createProjectUiState: StateFlow<UiState<Long>> = _createProjectUiState.asStateFlow()

    // (기존 createSongUiState 유지하되, 실제로 안 쓰면 삭제해도 됨)
    private val _createSongUiState = MutableStateFlow<UiState<Long>>(UiState.None)
    private val _renameProjectUiState = MutableStateFlow<UiState<Unit>>(UiState.None)
    val renameProjectUiState: StateFlow<UiState<Unit>> = _renameProjectUiState.asStateFlow()

    private val _deleteProjectUiState = MutableStateFlow<UiState<Unit>>(UiState.None)

    val deleteProjectUiState: StateFlow<UiState<Unit>> = _deleteProjectUiState.asStateFlow()
    val createSongUiState: StateFlow<UiState<Long>> = _createSongUiState.asStateFlow()
    fun resetRenameProjectUiState() { _renameProjectUiState.value = UiState.None }
    fun resetDeleteProjectUiState() { _deleteProjectUiState.value = UiState.None }
    fun resetCreateTeamspaceUiState() { _createTeamspaceUiState.value = UiState.None }
    fun resetCreateProjectUiState() { _createProjectUiState.value = UiState.None }
    fun resetCreateSongUiState() { _createSongUiState.value = UiState.None }

    // =========================
    // Home load
    // =========================
    fun loadHome(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _homeUiState.value = UiState.Loading
            }

            val lastId: String? = teamspaceRepository.lastTeamspaceId.first()

            when (val result = teamspaceRepository.getMyTeamspaces()) {
                is DataResult.Success -> {
                    val teamspaces = result.data
                    _teamspaces.value = teamspaces

                    if (teamspaces.isEmpty()) {
                        teamspaceRepository.clearLastTeamspaceId()
                        _currentTeamspaceName.value = ""
                        _homeUiState.value = UiState.Success(
                            HomeUiModel(
                                isLoading = false,
                                selectedTeamspace = null,
                                role = Role.MEMBER,
                                projects = emptyList(),
                                tracksCountByProjectId = emptyMap(),
                                errorMessage = null
                            )
                        )
                        return@launch
                    }

                    val selected = lastId?.let { id -> teamspaces.firstOrNull { it.id == id } }
                        ?: teamspaces.first()

                    teamspaceRepository.saveLastTeamspaceId(selected.id)
                    _currentTeamspaceName.value = selected.name

                    val projects: List<ProjectSummary> =
                        when (val p = projectRepository.getTeamspaceProjects(selected.id)) {
                            is DataResult.Success -> p.data
                            is DataResult.Error -> emptyList()
                        }

                    val counts = mutableMapOf<String, Int>()
                    for (p in projects) {
                        val count = when (val t = tracksRepository.getTracksList(p.id)) {
                            is DataResult.Success -> t.data.size
                            is DataResult.Error -> 0
                        }
                        counts[p.id] = count
                    }

                    _homeUiState.value = UiState.Success(
                        HomeUiModel(
                            isLoading = false,
                            selectedTeamspace = selected,
                            role = Role.MEMBER,
                            projects = projects,
                            tracksCountByProjectId = counts,
                            errorMessage = null
                        )
                    )
                }

                is DataResult.Error -> {
                    _currentTeamspaceName.value = ""
                    _homeUiState.value = UiState.Success(
                        HomeUiModel(
                            isLoading = false,
                            selectedTeamspace = null,
                            role = Role.MEMBER,
                            projects = emptyList(),
                            tracksCountByProjectId = emptyMap(),
                            errorMessage = result.throwable.message ?: "팀스페이스 조회 실패"
                        )
                    )
                }
            }
        }
    }
    // ✅ 스와이프 새로고침 전용
    fun refreshHome() {
        loadHome(showLoading = false)
    }
    fun loadCurrentTeamspaceName(teamspaceId: String) {
        if (teamspaceId.isBlank()) return

        viewModelScope.launch {
            when (val r = teamspaceRepository.getTeamspaceDetail(teamspaceId)) {
                is DataResult.Success -> _currentTeamspaceName.value = r.data.teamspaceName
                is DataResult.Error -> Unit
            }
        }
    }

    fun selectTeamspace(teamspaceId: String) {
        if (teamspaceId.isBlank()) return
        viewModelScope.launch {
            teamspaceRepository.saveLastTeamspaceId(teamspaceId)
            loadHome()
        }
    }

    // =========================
    // Create / Edit / Delete Project
    // =========================
    fun createTeamspace(name: String) {
        viewModelScope.launch {
            _createTeamspaceUiState.value = UiState.Loading

            when (val result = teamspaceRepository.createTeamspace(name)) {
                is DataResult.Success -> {
                    _createTeamspaceUiState.value = UiState.Success(System.currentTimeMillis())
                    loadHome()
                }

                is DataResult.Error -> {
                    _createTeamspaceUiState.value = UiState.Error(
                        message = result.throwable.message,
                        throwable = result.throwable
                    )
                }
            }
        }
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            _createProjectUiState.value = UiState.Loading

            val teamspaceId = (homeUiState.value as? UiState.Success)
                ?.data?.selectedTeamspace?.id.orEmpty()

            if (teamspaceId.isBlank()) {
                _createProjectUiState.value = UiState.Error("팀스페이스가 선택되지 않았습니다", null)
                return@launch
            }

            when (val r = projectRepository.createProject(teamspaceId, name)) {
                is DataResult.Success -> {
                    _createProjectUiState.value = UiState.Success(System.currentTimeMillis())
                    loadHome()
                }

                is DataResult.Error -> {
                    _createProjectUiState.value = UiState.Error(
                        message = r.throwable.message,
                        throwable = r.throwable
                    )
                }
            }
        }
    }

    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            _renameProjectUiState.value = UiState.Loading
            when (val r = projectRepository.editProjectName(projectId, newName)) {
                is DataResult.Success -> {
                    _renameProjectUiState.value = UiState.Success(Unit)
                    loadHome()
                }
                is DataResult.Error -> {
                    _renameProjectUiState.value = UiState.Error(
                        r.throwable.message ?: "프로젝트 이름 수정 실패",
                        r.throwable
                    )
                }
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            _deleteProjectUiState.value = UiState.Loading
            when (val r = projectRepository.deleteProject(projectId)) {
                is DataResult.Success -> {
                    _deleteProjectUiState.value = UiState.Success(Unit)
                    loadHome()
                }
                is DataResult.Error -> {
                    _deleteProjectUiState.value = UiState.Error(
                        r.throwable.message ?: "프로젝트 삭제 실패",
                        r.throwable
                    )
                }
            }
        }
    }

    // =========================
    // Tracks create / rename / delete
    // =========================
    private val _createTracksUiState = MutableStateFlow<UiState<Unit>>(UiState.None)
    val createTracksUiState: StateFlow<UiState<Unit>> = _createTracksUiState.asStateFlow()

    fun resetCreateTracksUiState() {
        _createTracksUiState.value = UiState.None
    }

    suspend fun getTracksListOnce(projectId: String): DataResult<List<TracksSummary>> {
        return tracksRepository.getTracksList(projectId)
    }

    fun createTracks(projectId: String, trackName: String) {
        viewModelScope.launch {
            _createTracksUiState.value = UiState.Loading

            when (val result = tracksRepository.createTracks(projectId, trackName)) {
                is DataResult.Success -> {
                    _createTracksUiState.value = UiState.Success(Unit)
                    // 구조 유지: 홈 전체 refresh (원하면 나중에 최적화)
                    loadHome()
                }
                is DataResult.Error -> {
                    _createTracksUiState.value = UiState.Error(
                        result.throwable.message ?: "트랙 생성 실패",
                        result.throwable
                    )
                }
            }
        }
    }

    // rename 성공 시: projectId를 emit해서 HomeFragment가 그 프로젝트만 다시 getTracksListOnce() 하게
    private val _tracksRefresh = MutableStateFlow<String?>(null)
    val tracksRefresh: StateFlow<String?> = _tracksRefresh.asStateFlow()

    fun clearTracksRefresh() {
        _tracksRefresh.value = null
    }

    fun renameTracks(projectId: String, tracksId: String, newName: String) {
        viewModelScope.launch {
            when (val r = tracksRepository.updateTracks(tracksId, newName)) {
                is DataResult.Success -> _tracksRefresh.value = projectId
                is DataResult.Error -> Unit
            }
        }
    }

    // ✅ 삭제 결과를 HomeFragment로 전달: Success면 (projectId, tracksId)
    private val _deleteTracksUiState =
        MutableStateFlow<UiState<Pair<String, String>>>(UiState.None)
    val deleteTracksUiState: StateFlow<UiState<Pair<String, String>>> =
        _deleteTracksUiState.asStateFlow()

    fun resetDeleteTracksUiState() {
        _deleteTracksUiState.value = UiState.None
    }

    fun deleteTracks(projectId: String, tracksId: String) {
        viewModelScope.launch {
            _deleteTracksUiState.value = UiState.Loading

            when (val res = tracksRepository.deleteTracks(tracksId)) {
                is DataResult.Success -> _deleteTracksUiState.value = UiState.Success(projectId to tracksId)
                is DataResult.Error -> _deleteTracksUiState.value = UiState.Error(
                    res.throwable.message ?: "delete failed",
                    res.throwable
                )
            }
        }
    }
}
