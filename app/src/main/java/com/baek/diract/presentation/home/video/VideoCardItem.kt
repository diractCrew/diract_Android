package com.baek.diract.presentation.home.video

import android.net.Uri
import com.baek.diract.domain.model.VideoSummary

/*
    비디오 카드 아이템의 상태를 나타내는 sealed class
 */
sealed class VideoCardItem {
    abstract val id: String
    abstract val sectionId: String

    // 서버에서 가져온 완료된 비디오
    data class Completed(val data: VideoSummary) : VideoCardItem() {
        override val id: String get() = data.id
        override val sectionId = data.sectionId
    }

    // 압축 진행 중
    data class Compressing(
        override val id: String,
        override val sectionId: String,
        val progress: Int // 0-100
    ) : VideoCardItem()

    // 업로드 진행 중
    data class Uploading(
        override val id: String,
        override val sectionId: String,
        val progress: Int // 0-100
    ) : VideoCardItem()

    // 실패 상태
    data class Failed(
        override val id: String,
        override val sectionId: String,
        val type: FailType,
        val message: String? = null,
        val retryInfo: RetryInfo? = null
    ) : VideoCardItem()
}
