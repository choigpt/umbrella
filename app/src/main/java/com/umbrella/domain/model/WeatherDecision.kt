package com.umbrella.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime

/**
 * 날씨 조회 결과 및 알림 결정
 */
sealed class WeatherDecision {

    /**
     * 비 예보 있음 - 알림 필요
     */
    data class RainExpected(
        val maxPop: Int,
        val location: Location,
        val notificationTime: LocalTime,
        val fetchedAt: Instant,
        val precipitationType: PrecipitationType = PrecipitationType.RAIN
    ) : WeatherDecision()

    /**
     * 비 예보 없음 - 알림 불필요
     */
    data class NoRain(
        val maxPop: Int,
        val threshold: Int,
        val location: Location,
        val fetchedAt: Instant
    ) : WeatherDecision()

    /**
     * 오류 발생
     */
    data class Error(
        val type: ErrorType,
        val message: String? = null,
        val cachedDecision: WeatherDecision? = null
    ) : WeatherDecision()
}

enum class ErrorType {
    NETWORK,
    LOCATION,
    API,
    UNKNOWN
}

/**
 * 알림 예약 결과
 */
sealed class ScheduleResult {

    data class Scheduled(
        val isExact: Boolean,
        val scheduledTime: LocalTime,
        val pop: Int
    ) : ScheduleResult()

    data object Cancelled : ScheduleResult()

    data class Failed(
        val reason: String
    ) : ScheduleResult()
}
