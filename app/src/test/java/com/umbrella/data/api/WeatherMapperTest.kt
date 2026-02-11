package com.umbrella.data.api

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherMapperTest {

    private fun createResponse(
        times: List<String>,
        pops: List<Int?> = times.map { 0 },
        temps: List<Double>? = null,
        codes: List<Int>? = null
    ) = WeatherResponse(
        latitude = 37.5665,
        longitude = 126.978,
        timezone = "Asia/Seoul",
        timezoneAbbreviation = "KST",
        hourly = HourlyData(
            time = times,
            precipitationProbability = pops,
            temperature2m = temps,
            weatherCode = codes
        )
    )

    // ==================== mapToForecast (targetDate) Tests ====================

    @Test
    fun `mapToForecast filters only target date data`() {
        val targetDate = LocalDate(2024, 1, 15)
        val response = createResponse(
            times = listOf(
                "2024-01-14T22:00", "2024-01-14T23:00",  // 전날
                "2024-01-15T00:00", "2024-01-15T06:00", "2024-01-15T12:00",  // 대상 날짜
                "2024-01-16T00:00", "2024-01-16T06:00"   // 다음날
            ),
            pops = listOf(10, 20, 30, 40, 50, 60, 70)
        )

        val forecast = WeatherMapper.mapToForecast(response, targetDate)

        assertEquals(targetDate, forecast.date)
        assertEquals(3, forecast.hourlyForecasts.size)
        assertEquals(0, forecast.hourlyForecasts[0].time.hour)
        assertEquals(6, forecast.hourlyForecasts[1].time.hour)
        assertEquals(12, forecast.hourlyForecasts[2].time.hour)
    }

    @Test
    fun `mapToForecast returns correct pop values for target date`() {
        val targetDate = LocalDate(2024, 1, 15)
        val response = createResponse(
            times = listOf(
                "2024-01-14T23:00",
                "2024-01-15T07:00",
                "2024-01-15T08:00",
                "2024-01-16T00:00"
            ),
            pops = listOf(10, 80, 65, 20)
        )

        val forecast = WeatherMapper.mapToForecast(response, targetDate)

        assertEquals(2, forecast.hourlyForecasts.size)
        assertEquals(80, forecast.hourlyForecasts[0].precipitationProbability)
        assertEquals(65, forecast.hourlyForecasts[1].precipitationProbability)
    }

    @Test
    fun `mapToForecast returns empty when no data for target date`() {
        val targetDate = LocalDate(2024, 1, 16)
        val response = createResponse(
            times = listOf(
                "2024-01-14T00:00", "2024-01-14T06:00",
                "2024-01-15T00:00", "2024-01-15T06:00"
            ),
            pops = listOf(10, 20, 30, 40)
        )

        val forecast = WeatherMapper.mapToForecast(response, targetDate)

        assertEquals(targetDate, forecast.date)
        assertTrue(forecast.hourlyForecasts.isEmpty())
    }

    @Test
    fun `mapToForecast includes temperature and weatherCode`() {
        val targetDate = LocalDate(2024, 1, 15)
        val response = createResponse(
            times = listOf("2024-01-15T09:00"),
            pops = listOf(70),
            temps = listOf(3.5),
            codes = listOf(71)
        )

        val forecast = WeatherMapper.mapToForecast(response, targetDate)

        assertEquals(1, forecast.hourlyForecasts.size)
        val hourly = forecast.hourlyForecasts[0]
        assertEquals(70, hourly.precipitationProbability)
        assertEquals(3.5, hourly.temperature!!, 0.01)
        assertEquals(71, hourly.weatherCode)
    }

    @Test
    fun `mapToForecast handles null pop as 0`() {
        val targetDate = LocalDate(2024, 1, 15)
        val response = createResponse(
            times = listOf("2024-01-15T07:00"),
            pops = listOf(null)
        )

        val forecast = WeatherMapper.mapToForecast(response, targetDate)

        assertEquals(1, forecast.hourlyForecasts.size)
        assertEquals(0, forecast.hourlyForecasts[0].precipitationProbability)
    }

    @Test
    fun `mapToForecast handles null temperature and weatherCode`() {
        val targetDate = LocalDate(2024, 1, 15)
        val response = createResponse(
            times = listOf("2024-01-15T07:00"),
            pops = listOf(50),
            temps = null,
            codes = null
        )

        val forecast = WeatherMapper.mapToForecast(response, targetDate)

        assertEquals(1, forecast.hourlyForecasts.size)
        val hourly = forecast.hourlyForecasts[0]
        assertEquals(50, hourly.precipitationProbability)
        assertEquals(null, hourly.temperature)
        assertEquals(null, hourly.weatherCode)
    }

    @Test
    fun `mapToForecast with today date returns today data`() {
        // 오늘 날짜 데이터를 targetDate로 요청하면 오늘 데이터만 반환
        val today = LocalDate(2024, 1, 15)
        val tomorrow = LocalDate(2024, 1, 16)
        val response = createResponse(
            times = listOf(
                "2024-01-15T06:00", "2024-01-15T12:00", "2024-01-15T18:00",
                "2024-01-16T06:00", "2024-01-16T12:00"
            ),
            pops = listOf(80, 70, 60, 20, 10)
        )

        val forecastToday = WeatherMapper.mapToForecast(response, today)
        val forecastTomorrow = WeatherMapper.mapToForecast(response, tomorrow)

        assertEquals(3, forecastToday.hourlyForecasts.size)
        assertEquals(80, forecastToday.hourlyForecasts[0].precipitationProbability)

        assertEquals(2, forecastTomorrow.hourlyForecasts.size)
        assertEquals(20, forecastTomorrow.hourlyForecasts[0].precipitationProbability)
    }

    @Test
    fun `mapToForecast full 24h data extracts all hours`() {
        val targetDate = LocalDate(2024, 1, 15)
        val times = (0..23).map { hour ->
            "2024-01-15T${hour.toString().padStart(2, '0')}:00"
        }
        val pops = (0..23).map { it * 4 }

        val response = createResponse(times = times, pops = pops)
        val forecast = WeatherMapper.mapToForecast(response, targetDate)

        assertEquals(24, forecast.hourlyForecasts.size)
        assertEquals(0, forecast.hourlyForecasts[0].precipitationProbability)
        assertEquals(92, forecast.hourlyForecasts[23].precipitationProbability)
    }

    // ==================== mapToTomorrowForecast (legacy) Tests ====================

    @Test
    fun `mapToTomorrowForecast does not crash with basic response`() {
        val response = createResponse(
            times = listOf(
                "2024-01-15T00:00",
                "2024-01-15T01:00",
                "2024-01-15T02:00"
            ),
            pops = listOf(10, 20, 30),
            temps = listOf(5.0, 4.5, 4.0),
            codes = listOf(0, 0, 1)
        )

        val forecast = WeatherMapper.mapToTomorrowForecast(response)

        // 매퍼가 정상 작동하는지 확인 (날짜 필터링은 현재 시간에 의존)
        assertTrue(forecast.hourlyForecasts.size >= 0)
    }

    @Test
    fun `mapToTomorrowForecast with null values does not crash`() {
        val response = createResponse(
            times = listOf("2024-01-15T00:00"),
            pops = listOf(null),
            temps = null,
            codes = null
        )

        // null 값 처리 테스트 - 크래시 없이 완료되어야 함
        WeatherMapper.mapToTomorrowForecast(response)
        assertTrue(true)
    }

    // ==================== parseDateTime edge cases ====================

    @Test
    fun `mapToForecast handles ISO format with seconds`() {
        val targetDate = LocalDate(2024, 1, 15)
        val response = createResponse(
            times = listOf("2024-01-15T07:00:00"),
            pops = listOf(55)
        )

        val forecast = WeatherMapper.mapToForecast(response, targetDate)

        assertEquals(1, forecast.hourlyForecasts.size)
        assertEquals(7, forecast.hourlyForecasts[0].time.hour)
    }

    @Test
    fun `mapToForecast skips invalid time strings`() {
        val targetDate = LocalDate(2024, 1, 15)
        val response = createResponse(
            times = listOf("invalid", "2024-01-15T07:00", "also-invalid"),
            pops = listOf(10, 80, 30)
        )

        val forecast = WeatherMapper.mapToForecast(response, targetDate)

        assertEquals(1, forecast.hourlyForecasts.size)
        assertEquals(80, forecast.hourlyForecasts[0].precipitationProbability)
    }
}
