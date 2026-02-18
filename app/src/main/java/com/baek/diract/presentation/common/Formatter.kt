package com.baek.diract.presentation.common

import android.content.Context
import com.baek.diract.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object Formatter {
    private val LOCALIZED_DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    private val STANDARD_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.mm.dd")
    fun LocalDate.toUiString(): String {
        return this.format(LOCALIZED_DATE_FORMATTER)
    }

    fun LocalDateTime.toUiString(): String {
        val formatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(Locale.getDefault())

        return this.format(formatter)
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

            else ->
                this.format(STANDARD_DATE_FORMATTER)
        }
    }
}
