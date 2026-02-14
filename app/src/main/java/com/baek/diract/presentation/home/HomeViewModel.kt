package com.baek.diract.presentation.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.ProjectSummary
import com.baek.diract.domain.repository.TeamspaceRepository
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val app: Application,
    private val teamspaceRepository: TeamspaceRepository,
) : ViewModel() {

    // ✅ 홈 화면 전체 상태
    private val _homeUiState = MutableStateFlow<UiState<HomeUiModel>>(UiState.Loading)
    val homeUiState: StateFlow<UiState<HomeUiModel>> = _homeUiState.asStateFlow()

    private val prefs = context.getSharedPreferences("home_tips", Context.MODE_PRIVATE)

    private companion object {
        const val KEY_PROJECT_TIP_STEP = "project_tip_step"          // 0,1,2
        const val KEY_EMPTY_PROJECT_TIP_DONE = "empty_project_done"  // boolean
        const val KEY_NO_TEAMSPACE_TIP_DONE = "no_teamspace_done"    // boolean
    }

    /**
     * ✅ "팀스페이스는 있는데 프로젝트 없음" 화면에서만 쓰는 2-step 툴팁
     * 0 -> 1 -> 2(종료)
     * 기본값은 2(꺼짐)으로 시작시키는 게 안전함
     */
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

    /**
     * ✅ "팀스페이스 없음" 툴팁은 별도 boolean으로 1회만
     */
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

    private val _createSongUiState = MutableStateFlow<UiState<Long>>(UiState.None)
    val createSongUiState: StateFlow<UiState<Long>> = _createSongUiState.asStateFlow()

    fun resetCreateTeamspaceUiState() { _createTeamspaceUiState.value = UiState.None }
    fun resetCreateProjectUiState() { _createProjectUiState.value = UiState.None }
    fun resetCreateSongUiState() { _createSongUiState.value = UiState.None }

    fun loadHome() {
        viewModelScope.launch {
            _homeUiState.value = UiState.Loading

            when (val result = teamspaceRepository.getMyTeamspaces()) {
                is DataResult.Success -> {
                    val teamspaces = result.data
                    val selected = teamspaces.firstOrNull()

                    val projects: List<ProjectSummary> = emptyList()
                    val role = Role.MEMBER

                    _homeUiState.value = UiState.Success(
                        HomeUiModel(
                            isLoading = false,
                            selectedTeamspace = selected,
                            role = role,
                            projects = projects,
                            errorMessage = null
                        )
                    )
                }

                is DataResult.Error -> {
                    _homeUiState.value = UiState.Success(
                        HomeUiModel(
                            isLoading = false,
                            selectedTeamspace = null,
                            projects = emptyList(),
                            errorMessage = result.throwable.message ?: "팀스페이스 조회 실패"
                        )
                    )
                }
            }
        }
    }

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
            delay(700)
            _createProjectUiState.value = UiState.Success(System.currentTimeMillis())
        }
    }

    fun createSong(name: String) {
        viewModelScope.launch {
            _createSongUiState.value = UiState.Loading
            delay(700)
            _createSongUiState.value = UiState.Success(System.currentTimeMillis())
        }
    }

    fun renameProject(projectId: String, newName: String) { /* TODO */ }
    fun deleteProject(projectId: String) { /* TODO */ }
}
