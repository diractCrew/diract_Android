package com.baek.diract.presentation.home.video.section

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.R
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.repository.VideoRepository
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SectionSettingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val videoRepository: VideoRepository,
) : ViewModel() {

    val tracksId: String = checkNotNull(savedStateHandle[KEY_TRACK_ID]) {
        "trackId값 없이 VideoList 접근이 불가능합니다."
    }
    val trackTitle: String? = savedStateHandle[KEY_TRACK_TITLE]

    private val _uiState = MutableStateFlow<UiState<Long>>(UiState.None)
    val uiState: StateFlow<UiState<Long>> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<Int>()
    val toastMessage: SharedFlow<Int> = _toastMessage.asSharedFlow()

    private val _sectionItems = MutableStateFlow<List<SectionItem>>(emptyList())
    val sectionItems: StateFlow<List<SectionItem>> = _sectionItems

    val editingItem: StateFlow<SectionItem?> =
        sectionItems
            .map { list -> list.find { it.isEditing } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    init {
        loadSections()
    }

    private fun loadSections() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            when (val sectionsResult = videoRepository.getSections(tracksId)) {
                is DataResult.Success -> {
                    val sections = sectionsResult.data
                    _sectionItems.value = sections
                        .drop(1)
                        .map {
                            SectionItem(
                                id = it.id,
                                name = it.title
                            )
                        }
                    _uiState.value = UiState.Success(System.currentTimeMillis())
                }

                is DataResult.Error -> {
                    _uiState.value = UiState.Error(
                        message = sectionsResult.throwable.message,
                        throwable = sectionsResult.throwable
                    )
                }
            }
        }
    }

    fun getEditingPosition(): Int {
        val item = editingItem.value ?: return -1
        return sectionItems.value.indexOfFirst { it.id == item.id }
    }

    fun startAddSection() {
        if (editingItem.value != null) return
        _sectionItems.update { list ->
            list + SectionItem(id = NEW_ID, name = "", isEditing = true)
        }
    }

    fun startEditSection(item: SectionItem) {
        if (editingItem.value != null) return
        _sectionItems.update { list ->
            list.map { sectionItem ->
                if (sectionItem.id == item.id)
                    sectionItem.copy(isEditing = true)
                else
                    sectionItem.copy(isEditing = false)
            }
        }
    }

    fun completeEditSection(newName: String) {
        val item = editingItem.value
        if (newName.isBlank() || item == null) return

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val result = if (item.id == NEW_ID) {
                videoRepository.addSection(tracksId, newName)
            } else {
                videoRepository.editSection(tracksId, item.id, newName)
            }

            when (result) {
                is DataResult.Success -> {
                    loadSections()
                }

                is DataResult.Error -> {
                    _uiState.value = UiState.Success(System.currentTimeMillis())
                    _toastMessage.emit(R.string.error_edit_section_message)
                }
            }
        }
    }

    fun cancelEditing() {
        _sectionItems.update { list ->
            list.mapNotNull { item ->
                when {
                    item.isEditing && item.id == NEW_ID -> null
                    item.isEditing -> item.copy(isEditing = false)
                    else -> item
                }
            }
        }
    }

    fun deleteSection(item: SectionItem) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = videoRepository.deleteSection(tracksId, item.id)) {
                is DataResult.Success -> {
                    loadSections()
                }

                is DataResult.Error -> {
                    _uiState.value = UiState.Success(System.currentTimeMillis())
                    _toastMessage.emit(R.string.error_delete_section_message)
                }
            }
        }
    }

    companion object {
        private const val TAG = "VideoListViewModel"
        private const val KEY_TRACK_ID = "tracksId"
        private const val KEY_TRACK_TITLE = "tracksTitle"
        private const val NEW_ID = "NEW_SECTION_ITEM"
    }
}
