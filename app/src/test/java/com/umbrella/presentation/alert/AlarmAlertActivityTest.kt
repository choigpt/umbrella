package com.umbrella.presentation.alert

import com.umbrella.data.scheduler.AlarmSchedulerImpl
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmAlertActivityTest {

    @Test
    fun `EXTRA_POP constant has correct value`() {
        assertEquals("extra_pop", AlarmAlertActivity.EXTRA_POP)
    }

    @Test
    fun `EXTRA_PRECIP_TYPE constant has correct value`() {
        assertEquals("extra_precip_type", AlarmAlertActivity.EXTRA_PRECIP_TYPE)
    }

    @Test
    fun `EXTRA_POP matches AlarmSchedulerImpl EXTRA_POP`() {
        assertEquals(AlarmSchedulerImpl.EXTRA_POP, AlarmAlertActivity.EXTRA_POP)
    }

    @Test
    fun `EXTRA_PRECIP_TYPE matches AlarmSchedulerImpl EXTRA_PRECIP_TYPE`() {
        assertEquals(AlarmSchedulerImpl.EXTRA_PRECIP_TYPE, AlarmAlertActivity.EXTRA_PRECIP_TYPE)
    }
}
