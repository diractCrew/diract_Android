package com.baek.diract.data.datasource.remote

import android.net.Uri
import com.baek.diract.data.dto.SectionDto
import com.baek.diract.data.dto.VideoDto
import com.baek.diract.data.dto.VideoWithTrackDto

interface VideoRemoteDataSource {

    // 단일 비디오 정보 조회
    suspend fun getVideo(videoId: String): VideoDto

    // 섹션 목록 조회
    suspend fun getSections(tracksId: String): List<SectionDto>

    // 비디오 목록 조회
    suspend fun getVideos(tracksId: String, sectionId: String): List<VideoWithTrackDto>

    // 비디오 업로드 및 저장
    suspend fun addVideo(
        tracksId: String,
        sectionId: String,
        videoUri: Uri,
        thumbnailUri: Uri,
        title: String,
        duration: Double,
        uploaderId: String,
        onProgress: ((Int) -> Unit)? = null
    )

    // 비디오 타이틀 수정
    suspend fun editVideoTitle(videoId: String, editedTitle: String)

    // 비디오 섹션 이동
    suspend fun moveVideoSection(
        tracksId: String,
        fromSectionId: String,
        toSectionId: String,
        trackId: String
    )

    // 비디오 삭제
    suspend fun deleteVideo(
        tracksId: String,
        sectionId: String,
        trackId: String,
        videoId: String
    )

    // 섹션 추가
    suspend fun addSection(tracksId: String, title: String): SectionDto

    // 섹션 수정
    suspend fun editSection(tracksId: String, sectionId: String, title: String): SectionDto

    // 섹션 삭제
    suspend fun deleteSection(tracksId: String, sectionId: String)
}
