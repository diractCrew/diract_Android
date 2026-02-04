package com.baek.diract.presentation.common

import android.content.Context
import androidx.annotation.StringRes
import com.baek.diract.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration

object Formatter {
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")

    fun LocalDate.toUiString(): String {
        return this.format(DATE_FORMATTER)
    }

    fun Double.toTimeString(): String {
        val totalSeconds = this.toInt()
        val minutes = totalSeconds / 60
        val secs = totalSeconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    fun Long.toTimeString(): String {
        val totalSeconds = this / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun LocalDateTime.toTimeAgoString(context: Context): String {
        val now = LocalDateTime.now()
        val duration = java.time.Duration.between(this, now)

        return when {
            duration.toMinutes() < 1 ->
                context.getString(R.string.time_just_now)

            duration.toMinutes() < 60 ->
                context.getString(R.string.time_minutes_ago, duration.toMinutes())

            duration.toHours() < 24 ->
                context.getString(R.string.time_hours_ago, duration.toHours())

            duration.toDays() < 7 ->
                context.getString(R.string.time_days_ago, duration.toDays())

            duration.toDays() < 30 ->
                context.getString(R.string.time_weeks_ago, duration.toDays() / 7)

            duration.toDays() < 365 ->
                context.getString(R.string.time_months_ago, duration.toDays() / 30)

            else ->
                context.getString(R.string.time_years_ago, duration.toDays() / 365)
        }
    }
}
