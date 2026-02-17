package com.baek.diract.data.repository

import com.baek.diract.data.mapper.toDomain
import com.baek.diract.data.remote.api.TracksApi
import com.baek.diract.data.remote.api.TracksNameRequest
import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.TracksSummary
import com.baek.diract.domain.repository.TracksRepository
import javax.inject.Inject

class TracksRepositoryImpl @Inject constructor(
    private val tracksApi: TracksApi
) : TracksRepository {

    override suspend fun createTracks(projectId: String, trackName: String): DataResult<TracksSummary> =
        safeCall {
            val res = tracksApi.createTracks(projectId, TracksNameRequest(trackName))
            res.data!!.toDomain()
        }

    override suspend fun getTracksList(projectId: String): DataResult<List<TracksSummary>> =
        safeCall {
            val res = tracksApi.getTracksList(projectId)
            res.data!!.map { it.toDomain() }
        }


    override suspend fun updateTracks(tracksId: String, trackName: String): DataResult<TracksSummary> =
        safeCall {
            val res = tracksApi.updateTracks(tracksId, TracksNameRequest(trackName))
            res.data!!.toDomain()
        }

    override suspend fun deleteTracks(tracksId: String): DataResult<Unit> =
        safeCall {
            tracksApi.deleteTracks(tracksId)
            Unit
        }

    private inline fun <T> safeCall(block: () -> T): DataResult<T> {
        return try {
            DataResult.Success(block())
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}