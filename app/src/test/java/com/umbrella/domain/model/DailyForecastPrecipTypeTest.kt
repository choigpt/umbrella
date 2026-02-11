package com.umbrella.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyForecastPrecipTypeTest {

    // ==================== dominantPrecipitationType ====================

    @Test
    fun `dominantPrecipitationType returns RAIN when only rain codes in range`() {
        val daily = createDailyForecast(
            listOf(
                HourlyData(5, 80, 61),  // rain
                HourlyData(6, 70, 63),  // rain
                HourlyData(7, 60, 80),  // rain shower
                HourlyData(8, 50, 51),  // drizzle
            )
        )
        assertEquals(PrecipitationType.RAIN, daily.dominantPrecipitationType(5, 9))
    }

    @Test
    fun `dominantPrecipitationType returns SNOW when only snow codes in range`() {
        val daily = createDailyForecast(
            listOf(
                HourlyData(5, 80, 71),  // snow
                HourlyData(6, 70, 73),  // snow
                HourlyData(7, 60, 85),  // snow shower
            )
        )
        assertEquals(PrecipitationType.SNOW, daily.dominantPrecipitationType(5, 8))
    }

    @Test
    fun `dominantPrecipitationType returns MIXED when rain and snow codes present`() {
        val daily = createDailyForecast(
            listOf(
                HourlyData(5, 80, 61),  // rain
                HourlyData(6, 70, 71),  // snow
                HourlyData(7, 60, 63),  // rain
            )
        )
        assertEquals(PrecipitationType.MIXED, daily.dominantPrecipitationType(5, 8))
    }

    @Test
    fun `dominantPrecipitationType returns MIXED when MIXED codes present`() {
        val daily = createDailyForecast(
            listOf(
                HourlyData(5, 80, 66),  // freezing rain (MIXED)
                HourlyData(6, 70, 67),  // freezing rain (MIXED)
            )
        )
        assertEquals(PrecipitationType.MIXED, daily.dominantPrecipitationType(5, 7))
    }

    @Test
    fun `dominantPrecipitationType returns MIXED when MIXED plus rain codes present`() {
        val daily = createDailyForecast(
            listOf(
                HourlyData(5, 80, 61),  // rain
                HourlyData(6, 70, 56),  // freezing drizzle (MIXED)
            )
        )
        // MIXED is in the set of types, so result should be MIXED
        assertEquals(PrecipitationType.MIXED, daily.dominantPrecipitationType(5, 7))
    }

    @Test
    fun `dominantPrecipitationType returns RAIN when no precipitation codes in range`() {
        val daily = createDailyForecast(
            listOf(
                HourlyData(5, 80, 0),   // clear
                HourlyData(6, 70, 1),   // mainly clear
                HourlyData(7, 60, 45),  // fog
            )
        )
        // Default: RAIN when no precipitation types found
        assertEquals(PrecipitationType.RAIN, daily.dominantPrecipitationType(5, 8))
    }

    @Test
    fun `dominantPrecipitationType returns RAIN when no forecasts in range`() {
        val daily = createDailyForecast(
            listOf(
                HourlyData(12, 80, 71),  // snow at noon
                HourlyData(13, 70, 73),  // snow
            )
        )
        // Range 5-8 has no data → default RAIN
        assertEquals(PrecipitationType.RAIN, daily.dominantPrecipitationType(5, 8))
    }

    @Test
    fun `dominantPrecipitationType returns RAIN when weatherCodes are null`() {
        val daily = createDailyForecast(
            listOf(
                HourlyData(5, 80, null),
                HourlyData(6, 70, null),
            )
        )
        assertEquals(PrecipitationType.RAIN, daily.dominantPrecipitationType(5, 7))
    }

    @Test
    fun `dominantPrecipitationType only considers forecasts within range`() {
        val daily = createDailyForecast(
            listOf(
                HourlyData(4, 80, 71),   // snow - OUTSIDE range
                HourlyData(5, 70, 61),   // rain - in range
                HourlyData(6, 60, 63),   // rain - in range
                HourlyData(9, 80, 75),   // snow - OUTSIDE range
            )
        )
        // Only 5-8 range: rain only
        assertEquals(PrecipitationType.RAIN, daily.dominantPrecipitationType(5, 9))
    }

    @Test
    fun `dominantPrecipitationType handles single snow forecast`() {
        val daily = createDailyForecast(
            listOf(
                HourlyData(7, 90, 75),  // snow
            )
        )
        assertEquals(PrecipitationType.SNOW, daily.dominantPrecipitationType(5, 10))
    }

    // ==================== Helper ====================

    private data class HourlyData(val hour: Int, val pop: Int, val weatherCode: Int?)

    private fun createDailyForecast(data: List<HourlyData>): DailyForecast {
        val forecasts = data.map { (hour, pop, code) ->
            HourlyForecast(
                time = LocalDateTime(2024, 1, 15, hour, 0),
                precipitationProbability = pop,
                weatherCode = code
            )
        }
        return DailyForecast(
            date = LocalDate(2024, 1, 15),
            hourlyForecasts = forecasts,
            fetchedAt = Clock.System.now()
        )
    }
}
