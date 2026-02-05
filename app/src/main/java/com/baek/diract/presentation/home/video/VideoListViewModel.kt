package com.baek.diract.presentation.home.video

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.R
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Section
import com.baek.diract.domain.model.VideoSummary
import com.baek.diract.domain.repository.AuthRepository
import com.baek.diract.domain.repository.VideoRepository
import com.baek.diract.domain.usecase.UploadVideoUseCase
import com.baek.diract.presentation.common.ToastEvent
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class VideoListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val videoRepository: VideoRepository,
    private val uploadVideoUseCase: UploadVideoUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    val tracksId: String = checkNotNull(savedStateHandle[KEY_TRACK_ID]) {
        "trackId값 없이 VideoList 접근이 불가능합니다."
    }
    val tracksTitle: String = checkNotNull(savedStateHandle[KEY_TRACK_TITLE]) {
        "trackId값 없이 VideoList 접근이 불가능합니다."
    }

    /*
        ----- 상태 플로우 -----
     */
    //원본 비디오
    private val _videos = MutableStateFlow<List<VideoSummary>>(emptyList())

    //업로드 중인 비디오
    private val _uploadingItems = MutableStateFlow<List<VideoCardItem>>(emptyList())

    // 원본 섹션 데이터
    private val _sections = MutableStateFlow<List<Section>>(emptyList())
    private val _selectedSectionId = MutableStateFlow<String?>(null)

    //로딩, 성공 실패 관리(로딩 상태 변화 관측을 위한 System.currentTimeMillis() 값 넣음)
    private val _uiState = MutableStateFlow<UiState<Long>>(UiState.None)
    val uiState: StateFlow<UiState<Long>> = _uiState.asStateFlow()

    //이름 수정 관리
    private val _editUiState = MutableStateFlow<UiState<Long>>(UiState.None)
    val editUiState: StateFlow<UiState<Long>> = _editUiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<ToastEvent>()
    val toastMessage: SharedFlow<ToastEvent> = _toastMessage.asSharedFlow()

    // 비디오 아이템
    val videoItems: StateFlow<List<VideoCardItem>> =
        combine(
            _videos,
            _uploadingItems,
            _selectedSectionId
        ) { videos, uploading, sectionId ->

            if (sectionId == null) return@combine emptyList()

            val uploading = uploading.filter { it.sectionId == sectionId }
            val completed = videos
                .map { VideoCardItem.Completed(it) }

            uploading + completed
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // VideoListFragment용 섹션 칩
    val sectionChipItems: StateFlow<List<SectionChipItem>> = combine(
        _sections,
        _selectedSectionId
    ) { sections, selectedId ->
        buildList {
            add(SectionChipItem.SetSection)
            addAll(sections.map { section ->
                SectionChipItem.SectionUi(
                    id = section.id,
                    name = section.title,
                    isSelected = section.id == selectedId
                )
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 현재 선택된 섹션 ID 반환
    private fun getSelectedSectionId(): String? {
        return _selectedSectionId.value
    }

    /*
        초기화면 데이터
     */
    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            // 1. 섹션 로드
            when (val sectionsResult = videoRepository.getSections(tracksId)) {
                is DataResult.Success -> {
                    val sections = sectionsResult.data
                    _sections.value = sections

                    val defaultSectionId = getSelectedSectionId()
                        ?.takeIf { id -> sections.any { it.id == id } }
                        ?: sections.firstOrNull()?.id
                    _selectedSectionId.value = defaultSectionId

                    // 2. 선택된 섹션의 비디오 로드
                    if (defaultSectionId != null) {
                        loadVideosForSection(defaultSectionId)
                    } else {
                        _videos.value = emptyList()
                        _uiState.value = UiState.Success(System.currentTimeMillis())
                    }
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

    // 특정 섹션의 비디오 로드
    private suspend fun loadVideosForSection(sectionId: String) {
        when (val videosResult = videoRepository.getVideos(tracksId, sectionId)) {
            is DataResult.Success -> {
                _videos.value = videosResult.data
                _uiState.value = UiState.Success(System.currentTimeMillis())
            }

            is DataResult.Error -> {
                Log.e(TAG, "Failed to load videos", videosResult.throwable)
                _uiState.value = UiState.Error(
                    message = videosResult.throwable.message,
                    throwable = videosResult.throwable
                )
            }
        }
    }

    // 선택된 섹션에 대한 영상 리스트 새로고침
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val selectedSectionId = getSelectedSectionId()
            Log.d(TAG, "선택된 섹션: $selectedSectionId")
            if (selectedSectionId != null) {
                loadVideosForSection(selectedSectionId)
                return@launch
            }
            _uiState.value = UiState.Success(System.currentTimeMillis())
        }
    }


    // 섹션 선택
    fun selectSection(sectionId: String) {
        if (getSelectedSectionId() == sectionId) return

        _selectedSectionId.value = sectionId

        // 선택된 섹션의 비디오 로드
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            loadVideosForSection(sectionId)
        }
    }


    /*
        ---------- 비디오 업로드 ----------
     */

    // ----- 업로딩 아이템 관리 -----
    private fun updateUploadingItem(
        transform: (List<VideoCardItem>) -> List<VideoCardItem>
    ) {
        val cur = _uploadingItems.value
        _uploadingItems.value = transform(cur)
    }

    // 실패 아이템 취소 (리스트에서 제거)
    fun cancelFailedItem(item: VideoCardItem.Failed) {
        updateUploadingItem() { list -> list.filter { it.id != item.id } }
    }

    // ----- 업로드/재시도 -----

    // 비디오 업로드 (압축 -> 업로드)
    fun uploadVideo(videoUri: Uri, title: String) {
        val uploadId = UUID.randomUUID().toString()
        val sectionId = getSelectedSectionId() ?: return
        val uploaderId = authRepository.getCurrentUser()?.uid ?: return

        // 압축 상태로 리스트에 추가
        updateUploadingItem { list ->
            listOf(
                VideoCardItem.Compressing(
                    id = uploadId,
                    sectionId = sectionId,
                    progress = 0
                )
            ) + list
        }


        viewModelScope.launch {
            val result = uploadVideoUseCase(
                videoUri = videoUri,
                title = title,
                tracksId = tracksId,
                sectionId = sectionId,
                uploaderId = uploaderId,
                onStateChanged = { state ->
                    when (state) {
                        is UploadVideoUseCase.UploadState.Compressing -> {
                            updateCompressionProgress(uploadId, state.progress)
                        }

                        is UploadVideoUseCase.UploadState.Uploading -> {
                            changeToUploadingState(uploadId)
                            updateUploadProgress(uploadId, state.progress)
                        }

                        is UploadVideoUseCase.UploadState.Completed -> {
                            updateUploadingItem { list -> list.filter { it.id != uploadId } }
                            refresh()
                            viewModelScope.launch {
                                _toastMessage.emit(
                                    ToastEvent(R.string.toast_upload_video, false)
                                )
                            }
                        }

                        is UploadVideoUseCase.UploadState.Failed -> {
                            val failType = when (state.phase) {
                                UploadVideoUseCase.FailPhase.COMPRESSION -> FailType.COMPRESSION
                                UploadVideoUseCase.FailPhase.UPLOAD -> FailType.UPLOAD
                            }
                            val retryInfo = RetryInfo(
                                originalUri = videoUri,
                                title = title,
                                sectionId = sectionId,
                                compressedUri = state.compressedUri,
                                thumbnailUri = state.thumbnailUri,
                                duration = state.duration
                            )
                            changeToFailedState(uploadId, failType, state.message, retryInfo)
                        }
                    }
                }
            )

            if (result is DataResult.Error) {
                Log.e("VideoListViewModel", "Upload failed", result.throwable)
            }
        }
    }

    // 실패 아이템 재시도
    fun retryFailedItem(item: VideoCardItem.Failed) {
        val retryInfo = item.retryInfo ?: return
        val selectedSectionId = item.sectionId
        val uploaderId = authRepository.getCurrentUser()?.uid ?: return
        val retryId = item.id

        when (item.type) {
            // 압축 실패: 처음부터 다시 시작
            FailType.COMPRESSION -> {
                changeToCompressingState(retryId)
                viewModelScope.launch {
                    uploadVideoUseCase(
                        videoUri = retryInfo.originalUri,
                        title = retryInfo.title,
                        tracksId = tracksId,
                        sectionId = selectedSectionId,
                        uploaderId = uploaderId,
                        onStateChanged = { state ->
                            handleUploadState(retryId, state, retryInfo)
                        }
                    )
                }
            }
            // 업로드 실패: 업로드만 재시도
            FailType.UPLOAD -> {
                val compressedUri = retryInfo.compressedUri ?: return
                val thumbnailUri = retryInfo.thumbnailUri ?: return
                val duration = retryInfo.duration ?: return

                changeToUploadingState(retryId)
                viewModelScope.launch {
                    uploadVideoUseCase.uploadOnly(
                        compressedUri = compressedUri,
                        thumbnailUri = thumbnailUri,
                        duration = duration,
                        title = retryInfo.title,
                        tracksId = tracksId,
                        sectionId = selectedSectionId,
                        uploaderId = uploaderId,
                        onStateChanged = { state ->
                            handleUploadState(retryId, state, retryInfo)
                        }
                    )
                }
            }
            // EXCEEDED는 재시도 불가
            FailType.EXCEEDED -> return
        }
    }

    /*
        ----- 업로드 상태 변경 -----
     */
    // 업로드 상태 변경 처리 (공통 로직)
    private fun handleUploadState(
        uploadId: String,
        state: UploadVideoUseCase.UploadState,
        retryInfo: RetryInfo
    ) {
        when (state) {
            is UploadVideoUseCase.UploadState.Compressing -> {
                updateCompressionProgress(uploadId, state.progress)
            }

            is UploadVideoUseCase.UploadState.Uploading -> {
                changeToUploadingState(uploadId)
                updateUploadProgress(uploadId, state.progress)
            }

            is UploadVideoUseCase.UploadState.Completed -> {
                updateUploadingItem { list -> list.filter { it.id != uploadId } }
                refresh()
            }

            is UploadVideoUseCase.UploadState.Failed -> {
                val failType = when (state.phase) {
                    UploadVideoUseCase.FailPhase.COMPRESSION -> FailType.COMPRESSION
                    UploadVideoUseCase.FailPhase.UPLOAD -> FailType.UPLOAD
                }
                val updatedRetryInfo = retryInfo.copy(
                    compressedUri = state.compressedUri ?: retryInfo.compressedUri,
                    thumbnailUri = state.thumbnailUri ?: retryInfo.thumbnailUri,
                    duration = state.duration ?: retryInfo.duration
                )
                changeToFailedState(uploadId, failType, state.message, updatedRetryInfo)
            }
        }
    }

    // 압축 상태로 변경
    private fun changeToCompressingState(id: String) {
        updateUploadingItem { list ->
            list.map { item ->
                if (item.id == id) {
                    VideoCardItem.Compressing(id = id, item.sectionId, progress = 0)
                } else {
                    item
                }
            }
        }
    }

    // 업로드 상태로 변경
    private fun changeToUploadingState(id: String) {
        updateUploadingItem { list ->
            list.map { item ->
                if (item.id == id) {
                    VideoCardItem.Uploading(id = id, sectionId = item.sectionId, progress = 0)
                } else {
                    item
                }
            }
        }
    }

    // 실패 상태로 변경
    private fun changeToFailedState(
        id: String,
        type: FailType,
        message: String?,
        retryInfo: RetryInfo
    ) {
        updateUploadingItem { list ->
            list.map { item ->
                if (item.id == id) {
                    VideoCardItem.Failed(
                        id = id,
                        sectionId = item.sectionId,
                        type = type,
                        message = message,
                        retryInfo = retryInfo
                    )
                } else {
                    item
                }
            }
        }
    }

    // 압축 진행률 업데이트
    private fun updateCompressionProgress(id: String, progress: Int) {
        updateUploadingItem { list ->
            list.map { item ->
                if (item.id == id && item is VideoCardItem.Compressing) {
                    item.copy(progress = progress)
                } else {
                    item
                }
            }
        }
    }

    // 업로드 진행률 업데이트
    private fun updateUploadProgress(id: String, progress: Int) {
        updateUploadingItem { list ->
            list.map { item ->
                if (item.id == id && item is VideoCardItem.Uploading) {
                    item.copy(progress = progress)
                } else {
                    item
                }
            }
        }
    }

    fun getVideoType(item: VideoSummary): VideoType {
        val isUploader = item.uploaderId == authRepository.getCurrentUser()?.uid
        val hasNoPart = _sections.value.size <= 1
        return when {
            !isUploader -> VideoType.OTHER_USER_VIDEO
            hasNoPart -> VideoType.MY_VIDEO_NO_PART
            else -> VideoType.MY_VIDEO_DEFAULT
        }
    }

    //------ 비디오 이름 수정 ------

    fun editVideoName(videoId: String, newName: String) {
        viewModelScope.launch {
            _editUiState.value = UiState.Loading

            when (val result = videoRepository.editVideoTitle(videoId, newName)) {
                is DataResult.Success -> {
                    _editUiState.value = UiState.Success(System.currentTimeMillis())
                    refresh()
                }

                is DataResult.Error -> {
                    _editUiState.value = UiState.Error(
                        message = result.throwable.message,
                        throwable = result.throwable
                    )
                    _toastMessage.emit(ToastEvent(R.string.toast_failed_edit_video_title, true))
                }
            }
        }
    }

    fun resetEditUiState() {
        _editUiState.value = UiState.None
    }

    fun deleteVideo(video: VideoSummary) {
        val sectionId = getSelectedSectionId() ?: return
        viewModelScope.launch {
            when (videoRepository.deleteVideo(tracksId, sectionId, video.trackId, video.id)) {

                is DataResult.Success -> {
                    _toastMessage.emit(ToastEvent(R.string.toast_delete_video, false))
                    refresh()
                }

                is DataResult.Error -> {
                    _toastMessage.emit(ToastEvent(R.string.toast_failed_delete_video, true))
                }
            }

        }
    }

    companion object {
        private const val TAG = "VideoListViewModel"
        private const val KEY_TRACK_ID = "tracksId"
        private const val KEY_TRACK_TITLE = "tracksTitle"
    }
}
