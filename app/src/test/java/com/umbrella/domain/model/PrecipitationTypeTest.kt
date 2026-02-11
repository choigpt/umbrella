package com.umbrella.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrecipitationTypeTest {

    // ==================== fromWeatherCode ====================

    @Test
    fun `fromWeatherCode returns RAIN for drizzle codes 51, 53, 55`() {
        assertEquals(PrecipitationType.RAIN, PrecipitationType.fromWeatherCode(51))
        assertEquals(PrecipitationType.RAIN, PrecipitationType.fromWeatherCode(53))
        assertEquals(PrecipitationType.RAIN, PrecipitationType.fromWeatherCode(55))
    }

    @Test
    fun `fromWeatherCode returns RAIN for rain codes 61, 63, 65`() {
        assertEquals(PrecipitationType.RAIN, PrecipitationType.fromWeatherCode(61))
        assertEquals(PrecipitationType.RAIN, PrecipitationType.fromWeatherCode(63))
        assertEquals(PrecipitationType.RAIN, PrecipitationType.fromWeatherCode(65))
    }

    @Test
    fun `fromWeatherCode returns RAIN for shower codes 80, 81, 82`() {
        assertEquals(PrecipitationType.RAIN, PrecipitationType.fromWeatherCode(80))
        assertEquals(PrecipitationType.RAIN, PrecipitationType.fromWeatherCode(81))
        assertEquals(PrecipitationType.RAIN, PrecipitationType.fromWeatherCode(82))
    }

    @Test
    fun `fromWeatherCode returns SNOW for snow codes 71, 73, 75, 77`() {
        assertEquals(PrecipitationType.SNOW, PrecipitationType.fromWeatherCode(71))
        assertEquals(PrecipitationType.SNOW, PrecipitationType.fromWeatherCode(73))
        assertEquals(PrecipitationType.SNOW, PrecipitationType.fromWeatherCode(75))
        assertEquals(PrecipitationType.SNOW, PrecipitationType.fromWeatherCode(77))
    }

    @Test
    fun `fromWeatherCode returns SNOW for snow shower codes 85, 86`() {
        assertEquals(PrecipitationType.SNOW, PrecipitationType.fromWeatherCode(85))
        assertEquals(PrecipitationType.SNOW, PrecipitationType.fromWeatherCode(86))
    }

    @Test
    fun `fromWeatherCode returns MIXED for freezing drizzle codes 56, 57`() {
        assertEquals(PrecipitationType.MIXED, PrecipitationType.fromWeatherCode(56))
        assertEquals(PrecipitationType.MIXED, PrecipitationType.fromWeatherCode(57))
    }

    @Test
    fun `fromWeatherCode returns MIXED for freezing rain codes 66, 67`() {
        assertEquals(PrecipitationType.MIXED, PrecipitationType.fromWeatherCode(66))
        assertEquals(PrecipitationType.MIXED, PrecipitationType.fromWeatherCode(67))
    }

    @Test
    fun `fromWeatherCode returns null for null code`() {
        assertNull(PrecipitationType.fromWeatherCode(null))
    }

    @Test
    fun `fromWeatherCode returns null for clear sky code 0`() {
        assertNull(PrecipitationType.fromWeatherCode(0))
    }

    @Test
    fun `fromWeatherCode returns null for fog code 45`() {
        assertNull(PrecipitationType.fromWeatherCode(45))
    }

    @Test
    fun `fromWeatherCode returns null for thunderstorm code 95`() {
        assertNull(PrecipitationType.fromWeatherCode(95))
    }

    @Test
    fun `fromWeatherCode returns null for unknown code`() {
        assertNull(PrecipitationType.fromWeatherCode(999))
    }

    // ==================== HourlyForecast.precipitationType ====================

    @Test
    fun `HourlyForecast precipitationType returns RAIN for rain weather code`() {
        val forecast = HourlyForecast(
            time = kotlinx.datetime.LocalDateTime(2024, 1, 15, 7, 0),
            precipitationProbability = 80,
            weatherCode = 61
        )
        assertEquals(PrecipitationType.RAIN, forecast.precipitationType)
    }

    @Test
    fun `HourlyForecast precipitationType returns SNOW for snow weather code`() {
        val forecast = HourlyForecast(
            time = kotlinx.datetime.LocalDateTime(2024, 1, 15, 7, 0),
            precipitationProbability = 80,
            weatherCode = 71
        )
        assertEquals(PrecipitationType.SNOW, forecast.precipitationType)
    }

    @Test
    fun `HourlyForecast precipitationType returns null when weatherCode is null`() {
        val forecast = HourlyForecast(
            time = kotlinx.datetime.LocalDateTime(2024, 1, 15, 7, 0),
            precipitationProbability = 80,
            weatherCode = null
        )
        assertNull(forecast.precipitationType)
    }

    @Test
    fun `HourlyForecast precipitationType returns null for non-precipitation code`() {
        val forecast = HourlyForecast(
            time = kotlinx.datetime.LocalDateTime(2024, 1, 15, 7, 0),
            precipitationProbability = 80,
            weatherCode = 0 // clear sky
        )
        assertNull(forecast.precipitationType)
    }
}
