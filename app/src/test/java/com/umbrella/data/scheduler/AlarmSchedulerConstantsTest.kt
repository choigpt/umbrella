package com.umbrella.data.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSchedulerConstantsTest {

    @Test
    fun `EXTRA_POP has expected value`() {
        assertEquals("extra_pop", AlarmSchedulerImpl.EXTRA_POP)
    }

    @Test
    fun `EXTRA_PRECIP_TYPE has expected value`() {
        assertEquals("extra_precip_type", AlarmSchedulerImpl.EXTRA_PRECIP_TYPE)
    }

    @Test
    fun `ACTION_WEATHER_PRE_CHECK has expected value`() {
        assertEquals("com.umbrella.ACTION_WEATHER_PRE_CHECK", AlarmSchedulerImpl.ACTION_WEATHER_PRE_CHECK)
    }

    @Test
    fun `PRE_CHECK_OFFSET_MINUTES is 60`() {
        assertEquals(60, AlarmSchedulerImpl.PRE_CHECK_OFFSET_MINUTES)
    }

    @Test
    fun `INEXACT_BUFFER_MINUTES is 10`() {
        assertEquals(10, AlarmSchedulerImpl.INEXACT_BUFFER_MINUTES)
    }

    @Test
    fun `ScheduleInfo diagnostic string contains key info`() {
        val info = ScheduleInfo(
            targetTimeMillis = 1705312200000,  // some millis
            triggerTimeMillis = 1705311600000,
            isExact = false,
            bufferApplied = true,
            bufferMinutes = 10,
            pop = 75
        )

        val diagnostic = info.toDiagnosticString()
        assertTrue(diagnostic.contains("비정확"))
        assertTrue(diagnostic.contains("75%"))
        assertTrue(diagnostic.contains("10분 앞당김"))
    }

    @Test
    fun `ScheduleInfo bufferDeltaMillis calculates correctly`() {
        val info = ScheduleInfo(
            targetTimeMillis = 1000000L,
            triggerTimeMillis = 400000L,
            isExact = false,
            bufferApplied = true,
            bufferMinutes = 10,
            pop = 50
        )
        assertEquals(600000L, info.bufferDeltaMillis)
    }
}
