package com.baek.diract.domain.repository

import com.baek.diract.domain.common.DataResult
import com.baek.diract.domain.model.TracksSummary

interface TracksRepository {
    suspend fun createTracks(projectId: String, trackName: String): DataResult<TracksSummary>
    suspend fun getTracksList(projectId: String): DataResult<List<TracksSummary>>
    suspend fun updateTracks(tracksId: String, trackName: String): DataResult<TracksSummary>
    suspend fun deleteTracks(tracksId: String): DataResult<Unit>
}
