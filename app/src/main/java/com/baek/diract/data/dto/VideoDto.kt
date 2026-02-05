package com.baek.diract.data.dto

import com.baek.diract.domain.model.VideoSummary
import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class VideoDto(
    val video_id: String = "",
    val video_title: String = "",
    val video_duration: Double = 0.0,
    val video_url: String = "",
    val thumbnail_url: String = "",
    val uploader_id: String = "",
    val created_at: Timestamp? = null,
    val updated_at: Timestamp? = null
) {
    fun toSummaryDomain(track_id: String, section_id: String): VideoSummary = VideoSummary(
        id = video_id,
        title = video_title,
        duration = video_duration,
        thumbnailUrl = thumbnail_url,
        createdAt = created_at?.let {
            Instant.ofEpochSecond(it.seconds)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } ?: LocalDate.now(),
        trackId = track_id,
        sectionId = section_id,
        uploaderId = uploader_id
    )
}
