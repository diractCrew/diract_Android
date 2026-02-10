package com.baek.diract.domain.repository

import android.net.Uri
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Section
import com.baek.diract.domain.model.VideoPlay
import com.baek.diract.domain.model.VideoSummary

interface VideoRepository {

    // 특정 트랙(tracks: 곡)의 섹션 목록 조회
    suspend fun getSections(tracksId: String): DataResult<List<Section>>

    // 특정 트랙의 비디오 목록 조회
    suspend fun getVideos(
        tracksId: String,
        sectionId: String
    ): DataResult<List<VideoSummary>>

    // 비디오 추가
    suspend fun addVideo(
        tracksId: String,
        sectionId: String,
        videoUri: Uri,
        thumbnailUri: Uri,
        title: String,
        duration: Double,
        uploaderId: String,
        onProgress: ((Int) -> Unit)? = null
    ): DataResult<Unit>

    // 비디오 타이틀 수정
    suspend fun editVideoTitle(videoId: String, editedTitle: String): DataResult<Unit>

    //비디오 파트 이동
    suspend fun moveVideoSection(
        tracksId: String,
        fromSectionId: String,
        toSectionId: String,
        trackId: String
    ): DataResult<Unit>

    // 비디오 삭제
    suspend fun deleteVideo(
        tracksId: String,
        sectionId: String, trackId: String, videoId: String
    ): DataResult<Unit>

    // 섹션 추가
    suspend fun addSection(tracksId: String, title: String): DataResult<Section>

    // 섹션 수정
    suspend fun editSection(tracksId: String, sectionId: String, title: String): DataResult<Section>

    // 섹션 삭제
    suspend fun deleteSection(tracksId: String, sectionId: String): DataResult<Unit>

    // 비디오 정보 조회
    suspend fun getVideo(videoId: String): DataResult<VideoPlay>

    // 비디오 다운로드 (캐시 확인 후 필요시 다운로드)
    suspend fun downloadVideo(
        videoId: String,
        videoUrl: String,
        onProgress: ((Int) -> Unit)? = null
    ): DataResult<Uri>
}

/*
## Collection: `tracks`
**경로**: `tracks/{tracks_id}`
- 곡(노래)

## Sub-Collection: `section`
**경로**: `tracks/{tracks_id}/section/{section_id}`
- 파트별 섹션

## Sub-Sub-Collection: `track`
**경로**: `tracks/{track_id}/section/{section_id}/track/{track_id}`
- videoId를 담고 있는 컬렉션

## Collection: `video`
**경로**: `video/{video_id}`
- 비디오 정보를 담고 있음(영상과 썸네일 이미지 스토리지 경로, 영상 제목 등)

# Storage
영상 스토리지 경로: `/video/{video_id}`
썸네일 스토리지 경로: `/video/{video_id}`
 */
