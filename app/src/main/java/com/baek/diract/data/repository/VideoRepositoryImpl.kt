package com.baek.diract.data.repository

import android.net.Uri
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
import android.util.Log
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

    companion object {
        private const val TAG = "VideoRepositoryImpl"
    }

    override suspend fun getSections(tracksId: String): DataResult<List<Section>> = safeCall {
        sectionApi.getSectionList(tracksId).data?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getVideos(
        tracksId: String,
        sectionId: String
    ): DataResult<List<VideoSummary>> = safeCall {
        trackApi.getTrackList(tracksId, sectionId).data?.map { it.toVideoSummary() } ?: emptyList()
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
        return DataResult.Error(Exception("addVideo: api 미구현"))
    }

    override suspend fun editVideoTitle(
        videoId: String,
        editedTitle: String
    ): DataResult<Unit> = safeCall {
        val video = videoApi.getVideo(videoId).data
            ?: throw Exception("비디오 조회 실패")
        videoApi.editVideo(
            videoId,
            EditVideoRequest(
                videoTitle = editedTitle,
                videoUrl = video.videoUrl,
                thumbnailUrl = video.thumbnailUrl,
                videoDuration = video.videoDuration
            )
        )
        Unit
    }

    //TODO: api 구현되면 적용
    override suspend fun moveVideoSection(
        tracksId: String,
        fromSectionId: String,
        toSectionId: String,
        trackId: String
    ): DataResult<Unit> {
        return DataResult.Error(Exception("moveVideoSection: api 미구현"))
    }

    override suspend fun deleteVideo(
        tracksId: String,
        sectionId: String,
        trackId: String,
        videoId: String
    ): DataResult<Unit> = safeCall {
        coroutineScope {
            val deferredTrack = async { trackApi.deleteTrack(tracksId, sectionId, trackId) }
            val deferredVideo = async { videoApi.deleteVideo(videoId) }
            deferredTrack.await()
            deferredVideo.await()
        }
        Unit
    }

    override suspend fun addSection(tracksId: String, title: String): DataResult<Section> =
        safeCall {
            sectionApi.createSection(tracksId, CreateSectionRequest(title)).data?.toDomain()
                ?: throw Exception("섹션 추가 실패")
        }

    override suspend fun editSection(
        tracksId: String,
        sectionId: String,
        title: String
    ): DataResult<Section> = safeCall {
        sectionApi.editSection(tracksId, sectionId, EditSectionRequest(title)).data?.toDomain()
            ?: throw Exception("섹션 수정 실패")
    }

    override suspend fun deleteSection(tracksId: String, sectionId: String): DataResult<Unit> =
        safeCall {
            sectionApi.deleteSection(tracksId, sectionId)
            Unit
        }

    override suspend fun getVideo(videoId: String): DataResult<VideoPlay> = safeCall {
        videoApi.getVideo(videoId).data?.toPlayDomain()
            ?: throw Exception("비디오 조회 실패")
    }

    override suspend fun downloadVideo(
        videoId: String,
        videoUrl: String,
        onProgress: ((Int) -> Unit)?
    ): DataResult<Uri> = safeCall {
        videoCacheManager.downloadAndCache(videoId, videoUrl, onProgress)
    }

    private inline fun <T> safeCall(block: () -> T): DataResult<T> {
        return try {
            val result = block()
            Log.d(TAG, "Success")
            DataResult.Success(result)
        } catch (e: Exception) {
            val serverMessage = (e as? retrofit2.HttpException)
                ?.response()?.errorBody()?.string()
            Log.e(TAG, "serverMessage=$serverMessage", e)
            DataResult.Error(e)
        }
    }
}
