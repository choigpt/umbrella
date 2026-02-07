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

        // 2. 날씨 조회
        val weatherResult = weatherRepository.getTomorrowForecast(location, forceRefresh)
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

        // 3. 설정 조회
        val settings = preferencesRepository.settingsFlow.first()

        // 4. PoP 계산 (±2h 범위 내 최대값)
        val maxPop = forecast.maxPopInRange(
            startHour = settings.popCheckStartHour,
            endHour = settings.popCheckEndHour
        )

        // 5. 결정
        return if (maxPop >= settings.popThreshold) {
            WeatherDecision.RainExpected(
                maxPop = maxPop,
                location = location,
                notificationTime = settings.notificationTime,
                fetchedAt = Clock.System.now()
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
