package com.umbrella.domain.usecase

import com.umbrella.data.location.LocationFallbackChain
import com.umbrella.data.location.LocationResult
import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.data.repository.WeatherError
import com.umbrella.data.repository.WeatherRepository
import com.umbrella.data.repository.WeatherResult
import com.umbrella.domain.model.ErrorType
import com.umbrella.domain.model.WeatherDecision
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * 날씨 조회 및 PoP 계산 UseCase
 *
 * 1. 위치 획득 (Fallback 체인)
 * 2. API 조회 (캐시 활용)
 * 3. PoP 계산 (알림시간 ±2h 범위 내 최대값)
 * 4. 임계치 비교 후 결정 반환
 */
class CheckWeatherUseCase @Inject constructor(
    private val locationChain: LocationFallbackChain,
    private val weatherRepository: WeatherRepository,
    private val preferencesRepository: PreferencesRepository
) {

    suspend operator fun invoke(forceRefresh: Boolean = false): WeatherDecision {
        // 1. 위치 획득
        val locationResult = locationChain.getLocation()
        val location = when (locationResult) {
            is LocationResult.Success -> locationResult.location
            is LocationResult.PermissionRequired -> {
                return WeatherDecision.Error(ErrorType.LOCATION, "위치 권한이 필요합니다")
            }
            is LocationResult.ManualSettingRequired -> {
                return WeatherDecision.Error(ErrorType.LOCATION, "위치를 수동으로 설정해주세요")
            }
        }

        // 2. 알림 대상 날짜 결정
        //    자정~알림시간: "오늘" (알림 당일)
        //    알림시간 이후: "내일" (다음 날 알림 예약)
        val settings = preferencesRepository.settingsFlow.first()
        val tz = TimeZone.of("Asia/Seoul")
        val nowDateTime = Clock.System.now().toLocalDateTime(tz)
        val today = nowDateTime.date
        val targetDate = if (nowDateTime.hour < settings.notificationTime.hour ||
            (nowDateTime.hour == settings.notificationTime.hour && nowDateTime.minute < settings.notificationTime.minute)
        ) {
            today // 아직 알림시간 전 → 오늘 예보 확인
        } else {
            LocalDate.fromEpochDays(today.toEpochDays() + 1) // 알림시간 지남 → 내일 예보
        }

        // 3. 날씨 조회
        val weatherResult = weatherRepository.getTomorrowForecast(location, forceRefresh, targetDate)
        val forecast = when (weatherResult) {
            is WeatherResult.Success -> weatherResult.forecast
            is WeatherResult.SuccessWithWarning -> weatherResult.forecast
            is WeatherResult.Failure -> {
                val errorType = when (weatherResult.error) {
                    WeatherError.NETWORK -> ErrorType.NETWORK
                    WeatherError.API -> ErrorType.API
                    WeatherError.UNKNOWN -> ErrorType.UNKNOWN
                }
                return WeatherDecision.Error(errorType, weatherResult.message)
            }
        }

        // 4. PoP 계산 (±2h 범위 내 최대값)
        val maxPop = forecast.maxPopInRange(
            startHour = settings.popCheckStartHour,
            endHour = settings.popCheckEndHour
        )

        // 5. 강수 유형 판별
        val precipType = forecast.dominantPrecipitationType(
            startHour = settings.popCheckStartHour,
            endHour = settings.popCheckEndHour
        )

        // 6. 결정
        return if (maxPop >= settings.popThreshold) {
            WeatherDecision.RainExpected(
                maxPop = maxPop,
                location = location,
                notificationTime = settings.notificationTime,
                fetchedAt = Clock.System.now(),
                precipitationType = precipType
            )
        } else {
            WeatherDecision.NoRain(
                maxPop = maxPop,
                threshold = settings.popThreshold,
                location = location,
                fetchedAt = Clock.System.now()
            )
        }
    }
}
