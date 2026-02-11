package com.umbrella.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherDecisionTest {

    @Test
    fun `RainExpected default precipitationType is RAIN`() {
        val decision = WeatherDecision.RainExpected(
            maxPop = 80,
            location = Location(37.5665, 126.9780, "서울"),
            notificationTime = LocalTime(7, 30),
            fetchedAt = Clock.System.now()
        )
        assertEquals(PrecipitationType.RAIN, decision.precipitationType)
    }

    @Test
    fun `RainExpected with SNOW precipitationType`() {
        val decision = WeatherDecision.RainExpected(
            maxPop = 80,
            location = Location(37.5665, 126.9780, "서울"),
            notificationTime = LocalTime(7, 30),
            fetchedAt = Clock.System.now(),
            precipitationType = PrecipitationType.SNOW
        )
        assertEquals(PrecipitationType.SNOW, decision.precipitationType)
    }

    @Test
    fun `RainExpected with MIXED precipitationType`() {
        val decision = WeatherDecision.RainExpected(
            maxPop = 60,
            location = Location(37.5665, 126.9780, "서울"),
            notificationTime = LocalTime(7, 30),
            fetchedAt = Clock.System.now(),
            precipitationType = PrecipitationType.MIXED
        )
        assertEquals(PrecipitationType.MIXED, decision.precipitationType)
    }

    @Test
    fun `RainExpected preserves maxPop value`() {
        val decision = WeatherDecision.RainExpected(
            maxPop = 75,
            location = Location(37.5665, 126.9780),
            notificationTime = LocalTime(7, 30),
            fetchedAt = Clock.System.now(),
            precipitationType = PrecipitationType.SNOW
        )
        assertEquals(75, decision.maxPop)
    }

    @Test
    fun `NoRain does not have precipitationType`() {
        val decision = WeatherDecision.NoRain(
            maxPop = 20,
            threshold = 40,
            location = Location(37.5665, 126.9780, "서울"),
            fetchedAt = Clock.System.now()
        )
        assertEquals(20, decision.maxPop)
        assertEquals(40, decision.threshold)
    }
}
