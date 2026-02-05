package com.baek.diract.presentation.home.video

import android.net.Uri

// 재시도에 필요한 정보
data class RetryInfo(
    val originalUri: Uri,
    val title: String,
    val sectionId: String,
    val compressedUri: Uri? = null, // 압축 성공 후 업로드 실패 시에만 존재
    val thumbnailUri: Uri? = null, // 압축 성공 후 업로드 실패 시에만 존재
    val duration: Double? = null // 압축 성공 시 얻은 영상 길이
)
