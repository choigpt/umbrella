package com.umbrella.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

/**
 * 시간별 날씨 예보 데이터
 */
data class HourlyForecast(
    val time: LocalDateTime,
    val precipitationProbability: Int, // PoP, 0-100
    val temperature: Double? = null,
    val weatherCode: Int? = null
) {
    val isRainy: Boolean
        get() = precipitationProbability >= 30
}

/**
 * 하루 전체 예보 데이터
 */
data class DailyForecast(
    val date: kotlinx.datetime.LocalDate,
    val hourlyForecasts: List<HourlyForecast>,
    val fetchedAt: Instant
) {
    /**
     * 특정 시간 범위 내 최대 강수확률
     */
    fun maxPopInRange(startHour: Int, endHour: Int): Int {
        return hourlyForecasts
            .filter { it.time.hour in startHour until endHour }
            .maxOfOrNull { it.precipitationProbability } ?: 0
    }

    /**
     * 특정 시간 범위 내 평균 강수확률 (가중치 없음)
     */
    fun avgPopInRange(startHour: Int, endHour: Int): Int {
        val relevant = hourlyForecasts.filter { it.time.hour in startHour until endHour }
        if (relevant.isEmpty()) return 0
        return relevant.map { it.precipitationProbability }.average().toInt()
    }
}

/**
 * 위치 정보
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val source: LocationSource = LocationSource.GPS
)

enum class LocationSource {
    GPS,           // 현재 위치 (FusedLocation)
    CACHED,        // 캐시된 마지막 위치
    MANUAL         // 사용자 수동 설정
}
