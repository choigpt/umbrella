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
     * (하위 호환: 기존 호출자는 그대로 동작)
     */
    fun mapToTomorrowForecast(response: WeatherResponse): DailyForecast {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.of("Asia/Seoul")).date
        val tomorrow = LocalDate.fromEpochDays(today.toEpochDays() + 1)
        return mapToForecast(response, tomorrow)
    }

    /**
     * WeatherResponse -> 알림 대상 날짜의 DailyForecast
     *
     * 알림시간 기준:
     * - 자정~알림시간: "오늘" 예보 확인 (알림 당일)
     * - 알림시간 이후: "내일" 예보 확인 (다음 날)
     *
     * @param targetDate 확인할 날짜
     */
    fun mapToForecast(response: WeatherResponse, targetDate: LocalDate): DailyForecast {
        val now = Clock.System.now()
        val hourlyForecasts = mutableListOf<HourlyForecast>()

        response.hourly.time.forEachIndexed { index, timeString ->
            val dateTime = parseDateTime(timeString) ?: return@forEachIndexed

            // 대상 날짜 데이터만 필터링
            if (dateTime.date != targetDate) return@forEachIndexed

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
            date = targetDate,
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
