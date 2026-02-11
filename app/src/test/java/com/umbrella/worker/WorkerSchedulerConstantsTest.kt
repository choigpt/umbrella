package com.umbrella.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WorkerSchedulerConstantsTest {

    @Test
    fun `all work names are unique`() {
        val names = setOf(
            WeatherCheckWorker.WORK_NAME_MIDNIGHT,
            WeatherCheckWorker.WORK_NAME_MORNING,
            WeatherCheckWorker.WORK_NAME_MIDDAY,
            WeatherCheckWorker.WORK_NAME_EVENING
        )
        assertEquals(4, names.size)
    }

    @Test
    fun `WORK_NAME_MIDNIGHT has expected value`() {
        assertEquals("weather_check_midnight", WeatherCheckWorker.WORK_NAME_MIDNIGHT)
    }

    @Test
    fun `WORK_NAME_MORNING has expected value`() {
        assertEquals("weather_check_morning", WeatherCheckWorker.WORK_NAME_MORNING)
    }

    @Test
    fun `WORK_NAME_MIDDAY has expected value`() {
        assertEquals("weather_check_midday", WeatherCheckWorker.WORK_NAME_MIDDAY)
    }

    @Test
    fun `WORK_NAME_EVENING has expected value`() {
        assertEquals("weather_check_evening", WeatherCheckWorker.WORK_NAME_EVENING)
    }

    @Test
    fun `legacy work names preserved for backward compatibility`() {
        // These existed before and must not change to avoid orphaned workers
        assertEquals("weather_check_evening", WeatherCheckWorker.WORK_NAME_EVENING)
        assertEquals("weather_check_morning", WeatherCheckWorker.WORK_NAME_MORNING)
    }
}
