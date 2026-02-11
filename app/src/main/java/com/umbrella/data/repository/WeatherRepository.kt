package com.umbrella.data.repository

import com.umbrella.data.api.OpenMeteoApi
import com.umbrella.data.api.WeatherMapper
import com.umbrella.data.prefs.WeatherCache
import com.umbrella.domain.model.DailyForecast
import com.umbrella.domain.model.Location
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 날씨 데이터 Repository
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val api: OpenMeteoApi,
    private val cache: WeatherCache
) {

    /**
     * 날씨 예보 조회
     *
     * @param location 위치
     * @param forceRefresh true면 캐시 무시하고 새로 조회
     * @param targetDate 예보 대상 날짜 (null이면 내일)
     * @return 성공 시 예보 데이터, 실패 시 캐시 데이터 또는 예외
     */
    suspend fun getTomorrowForecast(
        location: Location,
        forceRefresh: Boolean = false,
        targetDate: LocalDate? = null
    ): WeatherResult {
        // 캐시 확인 (forceRefresh가 아닐 때만)
        if (!forceRefresh) {
            cache.getValidCache()?.let { cacheResult ->
                val forecast = if (targetDate != null) {
                    WeatherMapper.mapToForecast(cacheResult.response, targetDate)
                } else {
                    WeatherMapper.mapToTomorrowForecast(cacheResult.response)
                }
                return WeatherResult.Success(
                    forecast = forecast,
                    fromCache = true,
                    cacheAgeMinutes = cacheResult.ageMinutes.toInt()
                )
            }
        }

        // API 호출
        return try {
            val response = api.getHourlyForecast(
                latitude = location.latitude,
                longitude = location.longitude
            )

            // 캐시 저장
            cache.saveCache(response)

            val forecast = if (targetDate != null) {
                WeatherMapper.mapToForecast(response, targetDate)
            } else {
                WeatherMapper.mapToTomorrowForecast(response)
            }
            WeatherResult.Success(
                forecast = forecast,
                fromCache = false,
                cacheAgeMinutes = 0
            )
        } catch (e: Exception) {
            // API 실패 시 캐시 사용 시도
            cache.getValidCache()?.let { cacheResult ->
                val forecast = if (targetDate != null) {
                    WeatherMapper.mapToForecast(cacheResult.response, targetDate)
                } else {
                    WeatherMapper.mapToTomorrowForecast(cacheResult.response)
                }
                return WeatherResult.SuccessWithWarning(
                    forecast = forecast,
                    warning = "네트워크 오류로 캐시 데이터 사용 중",
                    cacheAgeMinutes = cacheResult.ageMinutes.toInt()
                )
            }

            // 캐시도 없으면 실패
            WeatherResult.Failure(
                error = mapException(e),
                message = e.message
            )
        }
    }

    /**
     * 캐시 나이 문자열
     */
    suspend fun getCacheAgeString(): String? {
        return cache.getCacheAgeString()
    }

    private fun mapException(e: Exception): WeatherError {
        return when {
            e is java.net.UnknownHostException -> WeatherError.NETWORK
            e is java.net.SocketTimeoutException -> WeatherError.NETWORK
            e is retrofit2.HttpException -> WeatherError.API
            else -> WeatherError.UNKNOWN
        }
    }
}

sealed class WeatherResult {
    data class Success(
        val forecast: DailyForecast,
        val fromCache: Boolean,
        val cacheAgeMinutes: Int
    ) : WeatherResult()

    data class SuccessWithWarning(
        val forecast: DailyForecast,
        val warning: String,
        val cacheAgeMinutes: Int
    ) : WeatherResult()

    data class Failure(
        val error: WeatherError,
        val message: String?
    ) : WeatherResult()
}

enum class WeatherError {
    NETWORK,
    API,
    UNKNOWN
}
