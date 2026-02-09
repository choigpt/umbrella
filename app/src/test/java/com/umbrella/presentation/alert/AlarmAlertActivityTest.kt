package com.umbrella.presentation.alert

import com.umbrella.data.scheduler.AlarmSchedulerImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AlarmAlertActivityTest {

    @Test
    fun `EXTRA_POP constant has correct value`() {
        assertEquals("extra_pop", AlarmAlertActivity.EXTRA_POP)
    }

    @Test
    fun `EXTRA_POP matches AlarmSchedulerImpl EXTRA_POP`() {
        // AlarmReceiver에서 동일한 키로 전달하므로 일치해야 함
        assertEquals(AlarmSchedulerImpl.EXTRA_POP, AlarmAlertActivity.EXTRA_POP)
    }
}
