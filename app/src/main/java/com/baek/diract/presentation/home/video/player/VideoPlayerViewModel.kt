package com.baek.diract.presentation.home.video.player

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.data.util.VideoCacheManager
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.FeedbackUser
import com.baek.diract.domain.model.Feedback
import com.baek.diract.domain.model.VideoPlay
import com.baek.diract.domain.repository.AuthRepository
import com.baek.diract.domain.repository.FeedbackRepository
import com.baek.diract.domain.repository.VideoRepository
import com.baek.diract.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val videoRepository: VideoRepository,
    private val videoCacheManager: VideoCacheManager,
    private val feedbackRepository: FeedbackRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val videoId: String = checkNotNull(savedStateHandle[KEY_VIDEO_ID]) {
        "videoId값 없이 플레이어에 접근이 불가능합니다."
    }
    val videoTitle: String = savedStateHandle[KEY_VIDEO_TITLE] ?: ""
    val tracksTitle: String = savedStateHandle[KEY_TRACKS_TITLE] ?: ""


    // 비디오 UI 상태
    private val _videoState = MutableStateFlow<VideoPlayerState>(VideoPlayerState.Initial)
    val videoState: StateFlow<VideoPlayerState> = _videoState.asStateFlow()

    // 비디오 정보
    private val _videoInfo = MutableStateFlow<VideoPlay?>(null)
    val videoInfo: StateFlow<VideoPlay?> = _videoInfo.asStateFlow()

    // 재생 시점 관리
    private var _playbackPosition: Long = 0L
    val playbackPosition: Long get() = _playbackPosition

    private var _playWhenReady: Boolean = false
    val playWhenReady: Boolean get() = _playWhenReady

    // 재생 상태 저장
    fun savePlaybackState(position: Long, playWhenReady: Boolean) {
        _playbackPosition = position
        _playWhenReady = playWhenReady
    }

    init {
        loadVideo()
        loadFeedbacks()
    }

    // 비디오 로드 (캐시 확인 → 다운로드)
    fun loadVideo() {
        viewModelScope.launch {
            // 1. 캐시 확인
            val cachedUri = videoCacheManager.getCachedUri(videoId)
            if (cachedUri != null) {
                // 캐시 있음 → Loading 상태 (빠르게 로드)
                _videoState.value = VideoPlayerState.Loading
                loadVideoInfo(cachedUri)
                return@launch
            }

            // 2. 캐시 없음 → 비디오 정보 먼저 가져오기
            _videoState.value = VideoPlayerState.Loading

            when (val result = videoRepository.getVideo(videoId)) {
                is DataResult.Success -> {
                    _videoInfo.value = result.data
                    downloadVideo(result.data.videoUrl)
                }

                is DataResult.Error -> {
                    _videoState.value = VideoPlayerState.Error(
                        result.throwable.message ?: "비디오 정보를 가져올 수 없습니다."
                    )
                }
            }
        }
    }

    // 비디오 다운로드
    private suspend fun downloadVideo(videoUrl: String) {
        _videoState.value = VideoPlayerState.Downloading(0)

        when (val result = videoRepository.downloadVideo(
            videoId = videoId,
            videoUrl = videoUrl,
            onProgress = { progress ->
                _videoState.value = VideoPlayerState.Downloading(progress)
            }
        )) {
            is DataResult.Success -> {
                _videoState.value = VideoPlayerState.Ready(result.data)
            }

            is DataResult.Error -> {
                _videoState.value = VideoPlayerState.Error(
                    result.throwable.message ?: "비디오 다운로드에 실패했습니다."
                )
            }
        }
    }

    // 캐시된 비디오 정보 로드
    private suspend fun loadVideoInfo(cachedUri: Uri) {
        when (val result = videoRepository.getVideo(videoId)) {
            is DataResult.Success -> {
                _videoInfo.value = result.data
                _videoState.value = VideoPlayerState.Ready(cachedUri)
            }

            is DataResult.Error -> {
                _videoState.value = VideoPlayerState.Error(
                    result.throwable.message ?: "비디오 정보를 가져올 수 없습니다."
                )
            }
        }
    }

    // 재시도
    fun retry() {
        loadVideo()
    }

    /*
        피드백 관련
     */

    // 피드백 목록

    private val uid get() = authRepository.getCurrentUser()?.uid
    private val teamspaceId: String = "teamspaceId" //TODO:TeamspaceRepository에서 가져오기
    private val members: List<String> = emptyList() //TODO: TeamspaceRepository에서 가져오기

    private val feedbackState = MutableStateFlow<UiState<Long>>(UiState.None)
    private val feedbacks = MutableStateFlow<List<Feedback>>(emptyList())

    private val _feedbackItem = MutableStateFlow<List<FeedbackItem>>(emptyList())
    val feedbackItem: StateFlow<List<FeedbackItem>> = _feedbackItem.asStateFlow()

    // 피드백 목록 로드
    fun loadFeedbacks() {
        viewModelScope.launch {
            feedbackState.value = UiState.Loading

            when (val result = feedbackRepository.getFeedbacks(videoId)) {
                is DataResult.Success -> {
                    feedbackState.value = UiState.Success(
                        System.currentTimeMillis()
                    )
                    feedbacks.value = result.data
                }

                is DataResult.Error -> {
                    feedbackState.value = UiState.Error(result.throwable.message, result.throwable)
                }
            }

        }
    }

    //피드백 토글(전체 <-> 받은)
    fun filterFeedback(checked:Boolean) {
        val currentUid = uid ?: return
        val data = feedbacks.value

        val filtered = if (checked) {
            data.filter { feedback ->
                feedback.taggedUsers.any { it.userId == currentUid }
            }
        } else {
            data
        }
        _feedbackItem.value = filtered.map { it.toUiItem() }
    }


    // 피드백 작성
    fun uploadFeedback(
        content: String,
        startTime: Double,
        endTime: Double? = null,
        taggedUsers: List<FeedbackUser> = emptyList(),
        imgUrl: String? = null
    ) {
        //TODO: feedbackItem에 status가 Loading인거 추가 -> 성공시 피드백 새로고침, 실패시 Fail인거 추가

    }

    // 피드백 수정
    fun editFeedback(feedbackId: String, newContent: String) {
        viewModelScope.launch {
            when (feedbackRepository.editFeedback(feedbackId, newContent)) {
                is DataResult.Success -> {

                }

                is DataResult.Error -> {
                }
            }
        }
    }

    // 피드백 삭제
    fun deleteFeedback(feedbackId: String) {
        viewModelScope.launch {
            when (feedbackRepository.deleteFeedback(feedbackId)) {
                is DataResult.Success -> {
                    loadFeedbacks()
                }

                is DataResult.Error -> {
                }
            }
        }
    }

    // 피드백 신고
    fun reportFeedback(feedbackId: String) {
        viewModelScope.launch {
            when (feedbackRepository.reportFeedback(feedbackId)) {
                is DataResult.Success -> {
                }

                is DataResult.Error -> {
                }
            }
        }
    }

    companion object {
        private const val KEY_VIDEO_ID = "videoId"
        private const val KEY_TRACKS_TITLE = "tracksTitle"
        private const val KEY_VIDEO_TITLE = "videoTitle"

    }
}

