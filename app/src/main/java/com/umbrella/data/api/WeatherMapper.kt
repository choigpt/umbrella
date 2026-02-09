package com.umbrella.data.api

import com.umbrella.domain.model.DailyForecast
import com.umbrella.domain.model.HourlyForecast
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * API 응답을 Domain 모델로 변환
 */
object WeatherMapper {

    /**
     * WeatherResponse -> 내일 DailyForecast
     */
    fun mapToTomorrowForecast(response: WeatherResponse): DailyForecast {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.of("Asia/Seoul")).date
        val tomorrow = LocalDate.fromEpochDays(today.toEpochDays() + 1)

        val hourlyForecasts = mutableListOf<HourlyForecast>()

        response.hourly.time.forEachIndexed { index, timeString ->
            val dateTime = parseDateTime(timeString) ?: return@forEachIndexed

            // 내일 데이터만 필터링
            if (dateTime.date != tomorrow) return@forEachIndexed

            val pop = response.hourly.precipitationProbability.getOrNull(index) ?: 0
            val temp = response.hourly.temperature2m?.getOrNull(index)
            val code = response.hourly.weatherCode?.getOrNull(index)

            hourlyForecasts.add(
                HourlyForecast(
                    time = dateTime,
                    precipitationProbability = pop,
                    temperature = temp,
                    weatherCode = code
                )
            )
        }

        return DailyForecast(
            date = tomorrow,
            hourlyForecasts = hourlyForecasts,
            fetchedAt = now
        )
    }

    /**
     * ISO 8601 문자열 파싱 (2024-01-15T07:00)
     */
    private fun parseDateTime(isoString: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(isoString)
        } catch (e: Exception) {
            // "2024-01-15T07:00" 형식 시도
            try {
                val parts = isoString.split("T")
                if (parts.size != 2) return null

                val dateParts = parts[0].split("-")
                val timeParts = parts[1].split(":")

                LocalDateTime(
                    year = dateParts[0].toInt(),
                    monthNumber = dateParts[1].toInt(),
                    dayOfMonth = dateParts[2].toInt(),
                    hour = timeParts[0].toInt(),
                    minute = timeParts.getOrNull(1)?.toInt() ?: 0
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
