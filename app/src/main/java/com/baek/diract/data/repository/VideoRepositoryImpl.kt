package com.baek.diract.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.baek.diract.data.remote.api.CreateSectionRequest
import com.baek.diract.data.remote.api.CreateTrackRequest
import com.baek.diract.data.remote.api.CreateVideoRequest
import com.baek.diract.data.remote.api.EditSectionRequest
import com.baek.diract.data.remote.api.MoveTrackRequest
import com.baek.diract.data.remote.api.EditVideoRequest
import com.baek.diract.data.remote.api.SectionApi
import com.baek.diract.data.remote.api.TrackApi
import com.baek.diract.data.remote.api.VideoApi
import com.baek.diract.data.remote.dto.toDomain
import com.baek.diract.data.remote.dto.toPlayDomain
import com.baek.diract.data.remote.dto.toVideoSummary
import com.baek.diract.data.util.VideoCacheManager
import com.baek.diract.di.UploadClient
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Section
import com.baek.diract.domain.model.VideoPlay
import com.baek.diract.domain.model.VideoSummary
import com.baek.diract.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val videoCacheManager: VideoCacheManager,
    private val sectionApi: SectionApi,
    private val trackApi: TrackApi,
    private val videoApi: VideoApi,
    private val contentResolver: ContentResolver,
    @UploadClient private val uploadClient: OkHttpClient
) : VideoRepository {

    companion object {
        private const val TAG = "VideoRepositoryImpl"
    }

    override suspend fun getSections(tracksId: String): DataResult<List<Section>> = safeCall {
        sectionApi.getSectionList(tracksId).data
            ?.sortedBy { it.createdAt }
            ?.map { it.toDomain() }
            ?: emptyList()
    }

    override suspend fun getVideos(
        tracksId: String,
        sectionId: String
    ): DataResult<List<VideoSummary>> = safeCall {
        trackApi.getTrackList(tracksId, sectionId).data?.map { it.toVideoSummary() } ?: emptyList()
    }

    override suspend fun addVideo(
        tracksId: String,
        sectionId: String,
        videoUri: Uri,
        thumbnailUri: Uri,
        title: String,
        duration: Double,
        onProgress: ((Int) -> Unit)?
    ): DataResult<Unit> = safeCall {
        Log.d(TAG, "addVideo: 업로드 시작 (tracksId=${tracksId.take(8)}, sectionId=${sectionId.take(8)}, title=$title)")

        // Step 1: Signed URL 발급
        val uploadUrlResponse = videoApi.getUploadUrl().data
            ?: throw Exception("업로드 URL 발급 실패")
        Log.d(TAG, "addVideo: Signed URL 발급 완료 (videoId=${uploadUrlResponse.videoId.take(8)})")

        // Step 2: 영상/썸네일 병렬 업로드
        Log.d(TAG, "addVideo: 파일 업로드 시작")
        coroutineScope {
            val videoDeferred = async {
                uploadFile(uploadUrlResponse.videoUploadUrl, videoUri, "video/mp4", onProgress)
            }
            val thumbnailDeferred = async {
                uploadFile(uploadUrlResponse.thumbnailUploadUrl, thumbnailUri, "image/jpeg")
            }
            videoDeferred.await()
            thumbnailDeferred.await()
        }
        Log.d(TAG, "addVideo: 파일 업로드 완료")

        // Step 3: 메타데이터 저장 + 트랙 생성
        Log.d(TAG, "addVideo: 메타데이터 저장 시작")
        videoApi.createVideo(
            CreateVideoRequest(
                videoId = uploadUrlResponse.videoId,
                videoTitle = title,
                videoDuration = duration
            )
        )
        Log.d(TAG, "addVideo: 비디오 생성 완료, 트랙 생성 시작")
        trackApi.createTrack(tracksId, sectionId, CreateTrackRequest(
            uploadUrlResponse.videoId
        ))
        Log.d(TAG, "addVideo: 트랙 생성 완료")
        Unit
    }

    private suspend fun uploadFile(
        url: String,
        uri: Uri,
        contentType: String,
        onProgress: ((Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "uploadFile: 시작 (contentType=$contentType)")
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw Exception("파일을 열 수 없습니다: $uri")
        val fileSize = inputStream.available().toLong()
        Log.d(TAG, "uploadFile: 파일 크기=${fileSize}bytes")

        val requestBody = object : RequestBody() {
            override fun contentType() = contentType.toMediaType()
            override fun contentLength() = fileSize

            override fun writeTo(sink: BufferedSink) {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val source = stream.source()
                    val bufferSize = 8192L
                    var uploaded = 0L

                    while (true) {
                        val read = source.read(sink.buffer, bufferSize)
                        if (read == -1L) break
                        uploaded += read
                        sink.flush()
                        onProgress?.invoke((uploaded * 100 / fileSize).toInt())
                    }
                } ?: throw Exception("파일을 열 수 없습니다: $uri")
            }
        }

        inputStream.close()

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .header("Content-Type", contentType)
            .build()

        val response = uploadClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "uploadFile: 실패 (code=${response.code}, contentType=$contentType)")
            throw Exception("업로드 실패: ${response.code}")
        }
        Log.d(TAG, "uploadFile: 완료 (contentType=$contentType)")
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

    override suspend fun moveVideoSection(
        tracksId: String,
        fromSectionId: String,
        toSectionId: String,
        trackId: String
    ): DataResult<Unit> = safeCall {
        trackApi.moveTrack(tracksId, fromSectionId, trackId, MoveTrackRequest(toSectionId))
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
