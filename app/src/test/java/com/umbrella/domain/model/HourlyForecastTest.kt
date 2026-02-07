package com.umbrella.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class HourlyForecastTest {

    @Test
    fun `isRainy returns true when pop is 30 or above`() {
        val forecast = createForecast(precipitationProbability = 30)
        assertEquals(true, forecast.isRainy)
    }

    @Test
    fun `isRainy returns false when pop is below 30`() {
        val forecast = createForecast(precipitationProbability = 29)
        assertEquals(false, forecast.isRainy)
    }

    @Test
    fun `maxPopInRange returns maximum pop in given hour range`() {
        val dailyForecast = createDailyForecast(
            hourlyPops = listOf(
                5 to 10,   // 05:00 - 10%
                6 to 20,   // 06:00 - 20%
                7 to 65,   // 07:00 - 65% (max in range 5-9)
                8 to 40,   // 08:00 - 40%
                9 to 30,   // 09:00 - 30%
                10 to 80   // 10:00 - 80% (outside range)
            )
        )

        val maxPop = dailyForecast.maxPopInRange(5, 10)
        assertEquals(65, maxPop)
    }

    @Test
    fun `maxPopInRange returns 0 when no forecasts in range`() {
        val dailyForecast = createDailyForecast(
            hourlyPops = listOf(
                12 to 50,  // 12:00
                13 to 60   // 13:00
            )
        )

        val maxPop = dailyForecast.maxPopInRange(5, 10)
        assertEquals(0, maxPop)
    }

    @Test
    fun `avgPopInRange calculates average correctly`() {
        val dailyForecast = createDailyForecast(
            hourlyPops = listOf(
                5 to 10,   // 05:00
                6 to 20,   // 06:00
                7 to 30,   // 07:00
                8 to 40    // 08:00
            )
        )

        // Average of 10, 20, 30, 40 = 25
        val avgPop = dailyForecast.avgPopInRange(5, 9)
        assertEquals(25, avgPop)
    }

    private fun createForecast(precipitationProbability: Int): HourlyForecast {
        return HourlyForecast(
            time = LocalDateTime(2024, 1, 15, 7, 0),
            precipitationProbability = precipitationProbability
        )
    }

    private fun createDailyForecast(hourlyPops: List<Pair<Int, Int>>): DailyForecast {
        val forecasts = hourlyPops.map { (hour, pop) ->
            HourlyForecast(
                time = LocalDateTime(2024, 1, 15, hour, 0),
                precipitationProbability = pop
            )
        }

        return DailyForecast(
            date = LocalDate(2024, 1, 15),
            hourlyForecasts = forecasts,
            fetchedAt = Clock.System.now()
        )
    }
}
