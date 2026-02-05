package com.baek.diract.presentation.home.video.move_video

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.R
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Section
import com.baek.diract.domain.repository.VideoRepository
import com.baek.diract.presentation.common.ToastEvent
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoveVideoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val videoRepository: VideoRepository
) : ViewModel() {

    val tracksId: String = checkNotNull(savedStateHandle[KEY_TRACKS_ID]) {
        "tracksId값 없이 MoveVideo 접근이 불가능합니다."
    }

    val trackId: String = checkNotNull(savedStateHandle[KEY_TRACK_ID]) {
        "trackId값 없이 MoveVideo 접근이 불가능합니다."
    }

    private val defaultSectionId: String = checkNotNull(savedStateHandle[KEY_SECTION_ID]) {
        "sectionId값 없이 MoveVideo 접근이 불가능합니다."
    }

    private val _sections = MutableStateFlow<List<Section>>(emptyList())
    val sections: StateFlow<List<Section>> = _sections.asStateFlow()

    private val _selectedSectionId = MutableStateFlow(defaultSectionId)
    val selectedSectionId: StateFlow<String> = _selectedSectionId.asStateFlow()

    private val _loadState = MutableStateFlow<UiState<Unit>>(UiState.None)
    val loadState: StateFlow<UiState<Unit>> = _loadState.asStateFlow()

    private val _moveState = MutableStateFlow<UiState<String>>(UiState.None)
    val moveState: StateFlow<UiState<String>> = _moveState.asStateFlow()

    // 현재 섹션과 다른 섹션을 선택했는지 여부
    val isMoved: Boolean
        get() = _selectedSectionId.value != defaultSectionId

    private val _toastMessage = MutableSharedFlow<ToastEvent>()
    val toastMessage: SharedFlow<ToastEvent> = _toastMessage.asSharedFlow()

    init {
        loadSections()
    }

    //초기 데이터 로드
    private fun loadSections() {
        viewModelScope.launch {
            _loadState.value = UiState.Loading

            when (val result = videoRepository.getSections(tracksId)) {
                is DataResult.Success -> {
                    _sections.value = result.data
                    _loadState.value = UiState.Success(Unit)
                }

                is DataResult.Error -> {
                    _loadState.value = UiState.Error(
                        message = result.throwable.message,
                        throwable = result.throwable
                    )
                    _toastMessage.emit(ToastEvent(R.string.error_load_failed, true))
                }
            }
        }
    }

    fun selectSection(section: Section) {
        _selectedSectionId.value = section.id
    }

    fun moveVideo() {
        val toSectionId = _selectedSectionId.value
        if (toSectionId == defaultSectionId) return

        viewModelScope.launch {
            _moveState.value = UiState.Loading

            when (val result = videoRepository.moveVideoSection(
                tracksId = tracksId,
                fromSectionId = defaultSectionId,
                toSectionId = toSectionId,
                trackId = trackId
            )) {
                is DataResult.Success -> {
                    _moveState.value = UiState.Success(toSectionId)
                    _toastMessage.emit(ToastEvent(R.string.toast_move_video, false))
                }

                is DataResult.Error -> {
                    _moveState.value = UiState.Error(
                        message = result.throwable.message,
                        throwable = result.throwable
                    )
                    _toastMessage.emit(ToastEvent(R.string.toast_failed_move_video, true))
                }
            }
        }
    }

    companion object {
        const val KEY_TRACKS_ID = "tracksId"
        const val KEY_TRACK_ID = "trackId"
        const val KEY_SECTION_ID = "sectionId"
    }
}
