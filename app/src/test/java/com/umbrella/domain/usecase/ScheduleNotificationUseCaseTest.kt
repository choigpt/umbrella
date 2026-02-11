package com.umbrella.domain.usecase

import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.data.scheduler.AlarmScheduleResult
import com.umbrella.data.scheduler.NotificationScheduler
import com.umbrella.data.scheduler.ScheduleInfo
import com.umbrella.domain.model.Location
import com.umbrella.domain.model.PrecipitationType
import com.umbrella.domain.model.ScheduleResult
import com.umbrella.domain.model.WeatherDecision
import com.umbrella.notification.FailureNotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScheduleNotificationUseCaseTest {

    private lateinit var scheduler: NotificationScheduler
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var failureNotificationHelper: FailureNotificationHelper
    private lateinit var useCase: ScheduleNotificationUseCase

    @Before
    fun setup() {
        scheduler = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        failureNotificationHelper = mockk(relaxed = true)
        useCase = ScheduleNotificationUseCase(scheduler, preferencesRepository, failureNotificationHelper)
    }

    @Test
    fun `passes RAIN precipitationType to scheduler`() = runTest {
        val decision = createRainExpected(PrecipitationType.RAIN)
        coEvery { scheduler.scheduleNotification(any(), any(), any()) } returns createSuccess()

        useCase(decision)

        coVerify {
            scheduler.scheduleNotification(
                time = LocalTime(7, 30),
                pop = 80,
                precipitationType = PrecipitationType.RAIN
            )
        }
    }

    @Test
    fun `passes SNOW precipitationType to scheduler`() = runTest {
        val decision = createRainExpected(PrecipitationType.SNOW)
        coEvery { scheduler.scheduleNotification(any(), any(), any()) } returns createSuccess()

        useCase(decision)

        coVerify {
            scheduler.scheduleNotification(
                time = LocalTime(7, 30),
                pop = 80,
                precipitationType = PrecipitationType.SNOW
            )
        }
    }

    @Test
    fun `passes MIXED precipitationType to scheduler`() = runTest {
        val decision = createRainExpected(PrecipitationType.MIXED)
        coEvery { scheduler.scheduleNotification(any(), any(), any()) } returns createSuccess()

        useCase(decision)

        coVerify {
            scheduler.scheduleNotification(
                time = LocalTime(7, 30),
                pop = 80,
                precipitationType = PrecipitationType.MIXED
            )
        }
    }

    @Test
    fun `returns Scheduled on success`() = runTest {
        val decision = createRainExpected(PrecipitationType.RAIN)
        coEvery { scheduler.scheduleNotification(any(), any(), any()) } returns createSuccess()

        val result = useCase(decision)

        assertTrue(result is ScheduleResult.Scheduled)
    }

    @Test
    fun `returns Cancelled for NoRain`() = runTest {
        val decision = WeatherDecision.NoRain(
            maxPop = 20,
            threshold = 40,
            location = Location(37.5665, 126.9780, "서울"),
            fetchedAt = Clock.System.now()
        )

        val result = useCase(decision)

        assertTrue(result is ScheduleResult.Cancelled)
        coVerify { scheduler.cancelScheduledNotification() }
    }

    // ==================== Helpers ====================

    private fun createRainExpected(precipType: PrecipitationType) = WeatherDecision.RainExpected(
        maxPop = 80,
        location = Location(37.5665, 126.9780, "서울"),
        notificationTime = LocalTime(7, 30),
        fetchedAt = Clock.System.now(),
        precipitationType = precipType
    )

    private fun createSuccess() = AlarmScheduleResult.Success(
        ScheduleInfo(
            targetTimeMillis = System.currentTimeMillis() + 3600_000,
            triggerTimeMillis = System.currentTimeMillis() + 3600_000,
            isExact = true,
            bufferApplied = false,
            bufferMinutes = 0,
            pop = 80
        )
    )
}
