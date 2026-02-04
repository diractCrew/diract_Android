package com.baek.diract.presentation.home.video.player

import android.net.Uri

// 비디오 플레이어 UI 상태
sealed interface VideoPlayerState {
    // 초기 상태
    data object Initial : VideoPlayerState

    // 캐시에서 로딩 중 (빠름)
    data object Loading : VideoPlayerState

    // Storage에서 다운로드 중 (progress: 0~100)
    data class Downloading(val progress: Int) : VideoPlayerState

    // 재생 준비 완료
    data class Ready(val videoUri: Uri) : VideoPlayerState

    // 에러 발생
    data class Error(val message: String) : VideoPlayerState
}
