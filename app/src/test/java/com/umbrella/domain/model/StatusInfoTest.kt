package com.umbrella.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StatusInfoTest {

    @Test
    fun `scheduled status shows correct user message`() {
        val status = StatusInfo.scheduled(
            isExact = true,
            time = LocalTime(7, 30),
            pop = 65,
            locationName = "서울",
            lastUpdate = Clock.System.now()
        )

        assertEquals("알림 예약됨", status.userMessage)
        assertNotNull(status.detailMessage)
    }

    @Test
    fun `noRain status shows correct messages`() {
        val status = StatusInfo.noRain(
            pop = 20,
            threshold = 40,
            locationName = "서울",
            lastUpdate = Clock.System.now()
        )

        assertEquals("비 소식 없음", status.userMessage)
        assertEquals("강수확률 20%로 임계치(40%) 미만입니다", status.detailMessage)
    }

    @Test
    fun `error status shows correct messages`() {
        val status = StatusInfo.error(
            status = AppStatus.FETCH_FAILED_NETWORK,
            message = "Connection timeout",
            cacheAge = "2시간 30분"
        )

        assertEquals("날씨 정보 조회 실패", status.userMessage)
        assertNotNull(status.detailMessage)
    }

    @Test
    fun `checking status has no detail message`() {
        val status = StatusInfo.checking()

        assertEquals("날씨 확인 중...", status.userMessage)
        assertNull(status.detailMessage)
    }

    @Test
    fun `INITIAL status provides setup message`() {
        val status = StatusInfo.INITIAL

        assertEquals("설정을 완료하세요", status.userMessage)
        assertEquals("알림 시간과 강수확률 임계치를 설정하세요", status.detailMessage)
    }

    @Test
    fun `approximate scheduling indicates potential delay`() {
        val status = StatusInfo.scheduled(
            isExact = false,
            time = LocalTime(7, 30),
            pop = 50,
            locationName = "부산",
            lastUpdate = Clock.System.now()
        )

        assertEquals(AppStatus.SCHEDULED_APPROXIMATE, status.status)
        assertNotNull(status.detailMessage)
        // 상세 메시지에 "오차" 언급 확인
        assert(status.detailMessage!!.contains("오차"))
    }
}
