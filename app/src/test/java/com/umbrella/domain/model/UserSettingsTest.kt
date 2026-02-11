package com.umbrella.domain.model

import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class UserSettingsTest {

    @Test
    fun `default settings have correct values`() {
        val settings = UserSettings()

        assertEquals(LocalTime(7, 30), settings.notificationTime)
        assertEquals(40, settings.popThreshold)
        assertEquals(true, settings.isEnabled)
        assertEquals(null, settings.manualLocation)
    }

    @Test
    fun `popCheckStartHour is notification time minus 2 hours`() {
        val settings = UserSettings(notificationTime = LocalTime(7, 30))
        assertEquals(5, settings.popCheckStartHour)
    }

    @Test
    fun `popCheckStartHour does not go below 0`() {
        val settings = UserSettings(notificationTime = LocalTime(1, 0))
        assertEquals(0, settings.popCheckStartHour)
    }

    @Test
    fun `popCheckEndHour is notification time plus 2 hours`() {
        val settings = UserSettings(notificationTime = LocalTime(7, 30))
        assertEquals(9, settings.popCheckEndHour)
    }

    @Test
    fun `popCheckEndHour does not exceed 23`() {
        val settings = UserSettings(notificationTime = LocalTime(22, 0))
        assertEquals(23, settings.popCheckEndHour)
    }

    @Test
    fun `threshold constraints are correct`() {
        assertEquals(0, UserSettings.MIN_THRESHOLD)
        assertEquals(80, UserSettings.MAX_THRESHOLD)
        assertEquals(10, UserSettings.THRESHOLD_STEP)
    }
}
