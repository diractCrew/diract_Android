package com.baek.diract.data.dto

import com.baek.diract.domain.model.VideoSummary

data class VideoWithTrackDto(
    val trackId: String,
    val video: VideoDto
) {
    fun toSummaryDomain(sectionId:String): VideoSummary = video.toSummaryDomain(trackId,sectionId)
}
