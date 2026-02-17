package com.baek.diract.data.repository

import android.net.Uri
import android.util.Log
import com.baek.diract.data.remote.api.CreateSectionRequest
import com.baek.diract.data.remote.api.EditSectionRequest
import com.baek.diract.data.remote.api.SectionApi
import com.baek.diract.data.remote.api.TrackApi
import com.baek.diract.data.remote.api.EditVideoRequest
import com.baek.diract.data.remote.api.VideoApi
import com.baek.diract.data.remote.dto.toDomain
import com.baek.diract.data.remote.dto.toPlayDomain
import com.baek.diract.data.remote.dto.toVideoSummary
import com.baek.diract.data.util.VideoCacheManager
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Section
import com.baek.diract.domain.model.VideoPlay
import com.baek.diract.domain.model.VideoSummary
import com.baek.diract.domain.repository.VideoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val videoCacheManager: VideoCacheManager,
    private val sectionApi: SectionApi,
    private val trackApi: TrackApi,
    private val videoApi: VideoApi
) : VideoRepository {

    override suspend fun getSections(tracksId: String): DataResult<List<Section>> {
        return try {
            val response = sectionApi.getSectionList(tracksId)
            if (response.success) {
                DataResult.Success(response.data?.map { it.toDomain() } ?: emptyList())
            } else {
                val message = response.message ?: "섹션 목록 조회 실패"
                Log.e(TAG, "getSections failed: $message")
                DataResult.Error(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getSections error", e)
            DataResult.Error(e)
        }
    }

    override suspend fun getVideos(
        tracksId: String,
        sectionId: String
    ): DataResult<List<VideoSummary>> {
        return try {
            val response = trackApi.getTrackList(tracksId, sectionId)
            if (response.success) {
                DataResult.Success(response.data?.map { it.toVideoSummary() } ?: emptyList())
            } else {
                val message = response.message ?: "비디오 목록 조회 실패"
                Log.e(TAG, "getVideos failed: $message")
                DataResult.Error(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getVideos error", e)
            DataResult.Error(e)
        }
    }

    //TODO: api 수정 완료 시 적용하기
    override suspend fun addVideo(
        tracksId: String,
        sectionId: String,
        videoUri: Uri,
        thumbnailUri: Uri,
        title: String,
        duration: Double,
        uploaderId: String,
        onProgress: ((Int) -> Unit)?
    ): DataResult<Unit> {

        Log.e(TAG, "addVideo: api 미구현")
        return DataResult.Error(Exception("addVideo: api 미구현"))
    }

    override suspend fun editVideoTitle(
        videoId: String,
        editedTitle: String
    ): DataResult<Unit> {
        return try {
            // 현재 비디오 데이터 조회
            val getResponse = videoApi.getVideo(videoId)
            if (!getResponse.success || getResponse.data == null) {
                val message = getResponse.message ?: "비디오 조회 실패"
                Log.e(TAG, "editVideoTitle getVideo failed: $message")
                return DataResult.Error(Exception(message))
            }

            // 타이틀만 교체해서 수정 요청
            val editResponse = videoApi.editVideo(
                videoId,
                EditVideoRequest(
                    videoTitle = editedTitle,
                    videoUrl = getResponse.data.videoUrl,
                    thumbnailUrl = getResponse.data.thumbnailUrl,
                    videoDuration = getResponse.data.videoDuration
                )
            )
            if (editResponse.success) {
                DataResult.Success(Unit)
            } else {
                val message = editResponse.message ?: "비디오 타이틀 수정 실패"
                Log.e(TAG, "editVideoTitle failed: $message")
                DataResult.Error(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "editVideoTitle error", e)
            DataResult.Error(e)
        }
    }

    //TODO: api 구현되면 적용
    override suspend fun moveVideoSection(
        tracksId: String,
        fromSectionId: String,
        toSectionId: String,
        trackId: String
    ): DataResult<Unit> {
        Log.e(TAG, "moveVideoSection: api 미구현")
        return DataResult.Error(Exception("moveVideoSection: api 미구현"))
    }

    override suspend fun deleteVideo(
        tracksId: String,
        sectionId: String,
        trackId: String,
        videoId: String
    ): DataResult<Unit> {
        return try {
            coroutineScope {
                val deferredTrack = async { trackApi.deleteTrack(tracksId, sectionId, trackId) }
                val deferredVideo = async { videoApi.deleteVideo(videoId) }

                val responseTrack = deferredTrack.await()
                val responseVideo = deferredVideo.await()

                if (!responseTrack.success) {
                    val message = responseTrack.message ?: "트랙 삭제 실패"
                    Log.e(TAG, "deleteVideo deleteTrack failed: $message")
                    return@coroutineScope DataResult.Error(Exception(message))
                }
                if (!responseVideo.success) {
                    val message = responseVideo.message ?: "비디오 삭제 실패"
                    Log.e(TAG, "deleteVideo failed: $message")
                    return@coroutineScope DataResult.Error(Exception(message))
                }

                DataResult.Success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteVideo error", e)
            DataResult.Error(e)
        }
    }

    override suspend fun addSection(tracksId: String, title: String): DataResult<Section> {
        return try {
            val response = sectionApi.createSection(
                tracksId, CreateSectionRequest(
                    title
                )
            )
            if (response.success && response.data != null) {
                DataResult.Success(response.data.toDomain())
            } else {
                val message = response.message ?: "섹션 추가 실패"
                Log.e(TAG, "addSection failed: $message")
                DataResult.Error(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "addSection error", e)
            DataResult.Error(e)
        }
    }

    override suspend fun editSection(
        tracksId: String,
        sectionId: String,
        title: String
    ): DataResult<Section> {
        return try {
            val response = sectionApi.editSection(
                tracksId, sectionId, EditSectionRequest(
                    title
                )
            )
            if (response.success && response.data != null) {
                DataResult.Success(response.data.toDomain())
            } else {
                val message = response.message ?: "섹션 수정 실패"
                Log.e(TAG, "editSection failed: $message")
                DataResult.Error(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "editSection error", e)
            DataResult.Error(e)
        }
    }

    override suspend fun deleteSection(tracksId: String, sectionId: String): DataResult<Unit> {
        return try {
            val response = sectionApi.deleteSection(tracksId, sectionId)
            if (response.success) {
                DataResult.Success(Unit)
            } else {
                val message = response.message ?: "섹션 삭제 실패"
                Log.e(TAG, "deleteSection failed: $message")
                DataResult.Error(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteSection error", e)
            DataResult.Error(e)
        }
    }

    override suspend fun getVideo(videoId: String): DataResult<VideoPlay> {
        return try {
            val response = videoApi.getVideo(videoId)
            if (response.success && response.data != null) {
                DataResult.Success(response.data.toPlayDomain())
            } else {
                val message = response.message ?: "비디오 조회 실패"
                Log.e(TAG, "getVideo failed: $message")
                DataResult.Error(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getVideo error", e)
            DataResult.Error(e)
        }
    }

    override suspend fun downloadVideo(
        videoId: String,
        videoUrl: String,
        onProgress: ((Int) -> Unit)?
    ): DataResult<Uri> {
        return try {
            val uri = videoCacheManager.downloadAndCache(videoId, videoUrl, onProgress)
            DataResult.Success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "downloadVideo error", e)
            DataResult.Error(e)
        }
    }

    companion object {
        private const val TAG = "VideoRepositoryImpl"
    }
}
