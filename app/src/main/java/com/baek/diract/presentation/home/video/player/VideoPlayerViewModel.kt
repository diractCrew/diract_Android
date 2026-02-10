package com.baek.diract.presentation.home.video.player

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baek.diract.data.util.VideoCacheManager
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.VideoPlay
import com.baek.diract.domain.repository.VideoRepository
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
    private val videoCacheManager: VideoCacheManager
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

    private var _playbackSpeed: Float = 1.0f
    val playbackSpeed: Float get() = _playbackSpeed

    // 재생 상태 저장
    fun savePlaybackState(position: Long, playWhenReady: Boolean) {
        _playbackPosition = position
        _playWhenReady = playWhenReady
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed = speed
    }

    init {
        loadVideo()
    }

    // 비디오 로드 (캐시 확인 → 다운로드)
    fun loadVideo() {
        viewModelScope.launch {
            val cachedUri = videoCacheManager.getCachedUri(videoId)
            if (cachedUri != null) {
                _videoState.value = VideoPlayerState.Loading
                loadVideoInfo(cachedUri)
                return@launch
            }

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

    companion object {
        private const val KEY_VIDEO_ID = "videoId"
        private const val KEY_TRACKS_TITLE = "tracksTitle"
        private const val KEY_VIDEO_TITLE = "videoTitle"
    }
}
