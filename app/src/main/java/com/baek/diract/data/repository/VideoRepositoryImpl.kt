package com.baek.diract.data.repository

import android.net.Uri
import com.baek.diract.data.datasource.remote.VideoRemoteDataSource
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.Section
import com.baek.diract.domain.model.VideoSummary
import com.baek.diract.domain.repository.VideoRepository
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val remoteDataSource: VideoRemoteDataSource
) : VideoRepository {

    override suspend fun getSections(tracksId: String): DataResult<List<Section>> {
        return try {
            val sections = remoteDataSource.getSections(tracksId)
            DataResult.Success(sections.map { it.toDomain() })
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun getVideos(
        tracksId: String,
        sectionId: String
    ): DataResult<List<VideoSummary>> {
        return try {
            val videos = remoteDataSource.getVideos(tracksId, sectionId)
            DataResult.Success(videos.map { it.toSummaryDomain(sectionId) })
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

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
        return try {
            remoteDataSource.addVideo(
                tracksId,
                sectionId,
                videoUri,
                thumbnailUri,
                title,
                duration,
                uploaderId,
                onProgress
            )
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun editVideoTitle(
        videoId: String,
        editedTitle: String
    ): DataResult<Unit> {
        return try {
            remoteDataSource.editVideoTitle(videoId, editedTitle)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun moveVideoSection(
        tracksId: String,
        fromSectionId: String,
        toSectionId: String,
        trackId: String
    ): DataResult<Unit> {
        return try {
            remoteDataSource.moveVideoSection(tracksId, fromSectionId, toSectionId, trackId)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun deleteVideo(
        tracksId: String,
        sectionId: String,
        trackId: String,
        videoId: String
    ): DataResult<Unit> {
        return try {
            remoteDataSource.deleteVideo(tracksId, sectionId, trackId, videoId)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun addSection(
        tracksId: String,
        title: String
    ): DataResult<Section> {
        return try {
            val section = remoteDataSource.addSection(tracksId, title)
            DataResult.Success(section.toDomain())
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun editSection(
        tracksId: String,
        sectionId: String,
        title: String
    ): DataResult<Section> {
        return try {
            val section = remoteDataSource.editSection(tracksId, sectionId, title)
            DataResult.Success(section.toDomain())
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun deleteSection(
        tracksId: String,
        sectionId: String
    ): DataResult<Unit> {
        return try {
            remoteDataSource.deleteSection(tracksId, sectionId)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
