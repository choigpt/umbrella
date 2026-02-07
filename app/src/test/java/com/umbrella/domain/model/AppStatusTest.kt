package com.umbrella.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStatusTest {

    @Test
    fun `fromCode returns correct status`() {
        assertEquals(AppStatus.SCHEDULED_EXACT, AppStatus.fromCode("SCHED_EXACT"))
        assertEquals(AppStatus.NO_RAIN_EXPECTED, AppStatus.fromCode("NO_RAIN"))
        assertEquals(AppStatus.FETCH_FAILED_NETWORK, AppStatus.fromCode("ERR_NETWORK"))
    }

    @Test
    fun `fromCode returns UNKNOWN for invalid code`() {
        assertEquals(AppStatus.UNKNOWN, AppStatus.fromCode("INVALID_CODE"))
    }

    @Test
    fun `error statuses have isError true`() {
        assertTrue(AppStatus.FETCH_FAILED_NETWORK.isError)
        assertTrue(AppStatus.FETCH_FAILED_LOCATION.isError)
        assertTrue(AppStatus.FETCH_FAILED_API.isError)
    }

    @Test
    fun `success statuses have isError false`() {
        assertFalse(AppStatus.SCHEDULED_EXACT.isError)
        assertFalse(AppStatus.SCHEDULED_APPROXIMATE.isError)
        assertFalse(AppStatus.NO_RAIN_EXPECTED.isError)
    }

    @Test
    fun `permission statuses require action`() {
        assertTrue(AppStatus.PERMISSION_MISSING_NOTIFICATION.requiresAction)
        assertTrue(AppStatus.PERMISSION_MISSING_LOCATION.requiresAction)
        assertTrue(AppStatus.INITIAL.requiresAction)
    }

    @Test
    fun `scheduled statuses do not require action`() {
        assertFalse(AppStatus.SCHEDULED_EXACT.requiresAction)
        assertFalse(AppStatus.SCHEDULED_APPROXIMATE.requiresAction)
    }
}
