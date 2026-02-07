package com.umbrella.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo API 인터페이스
 * https://open-meteo.com/en/docs
 *
 * 무료, API 키 불필요, 시간별 예보 지원
 */
interface OpenMeteoApi {

    /**
     * 시간별 날씨 예보 조회
     *
     * @param latitude 위도
     * @param longitude 경도
     * @param hourly 조회할 시간별 데이터 (precipitation_probability)
     * @param timezone 시간대 (Asia/Seoul)
     * @param forecastDays 예보 일수 (기본 2일: 오늘 + 내일)
     */
    @GET("v1/forecast")
    suspend fun getHourlyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "precipitation_probability,temperature_2m,weather_code",
        @Query("timezone") timezone: String = "Asia/Seoul",
        @Query("forecast_days") forecastDays: Int = 2
    ): WeatherResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }
}
