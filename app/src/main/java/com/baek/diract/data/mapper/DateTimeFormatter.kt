package com.baek.diract.data.mapper

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// "2026-02-07T16:59:51.403491300Z" 형태의 UTC 타임스탬프를 시스템 기본 시간대의 LocalDateTime으로 변환
fun parseDateTime(value: String): LocalDateTime =
    try {
        Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime()
    } catch (e: Exception) {
        LocalDateTime.now()
    }
