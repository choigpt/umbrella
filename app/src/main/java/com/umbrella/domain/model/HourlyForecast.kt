package com.umbrella.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

/**
 * 강수 유형
 */
enum class PrecipitationType {
    RAIN,   // 비
    SNOW,   // 눈
    MIXED;  // 비/눈 혼합

    companion object {
        // WMO Weather Code → PrecipitationType
        private val RAIN_CODES = setOf(51, 53, 55, 61, 63, 65, 80, 81, 82)
        private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)
        private val MIXED_CODES = setOf(56, 57, 66, 67)

        fun fromWeatherCode(code: Int?): PrecipitationType? {
            if (code == null) return null
            return when (code) {
                in RAIN_CODES -> RAIN
                in SNOW_CODES -> SNOW
                in MIXED_CODES -> MIXED
                else -> null
            }
        }
    }
}

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

    val precipitationType: PrecipitationType?
        get() = PrecipitationType.fromWeatherCode(weatherCode)
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

    /**
     * 특정 시간 범위 내 주요 강수 유형 판별
     * - 눈 코드만 → SNOW
     * - 비 코드만 → RAIN
     * - 둘 다 → MIXED
     * - 강수 코드 없음 → RAIN (기본값)
     */
    fun dominantPrecipitationType(startHour: Int, endHour: Int): PrecipitationType {
        val relevant = hourlyForecasts.filter { it.time.hour in startHour until endHour }
        val types = relevant.mapNotNull { it.precipitationType }.toSet()

        return when {
            types.isEmpty() -> PrecipitationType.RAIN // 기본값
            PrecipitationType.MIXED in types -> PrecipitationType.MIXED
            types.size > 1 -> PrecipitationType.MIXED // RAIN + SNOW
            else -> types.first()
        }
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
