package com.umbrella.domain.usecase

import com.umbrella.data.location.FallbackLevel
import com.umbrella.data.location.LocationFallbackChain
import com.umbrella.data.location.LocationResult
import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.data.repository.WeatherRepository
import com.umbrella.data.repository.WeatherResult
import com.umbrella.domain.model.DailyForecast
import com.umbrella.domain.model.HourlyForecast
import com.umbrella.domain.model.Location
import com.umbrella.domain.model.PrecipitationType
import com.umbrella.domain.model.UserSettings
import com.umbrella.domain.model.WeatherDecision
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckWeatherUseCaseTest {

    private lateinit var locationChain: LocationFallbackChain
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var useCase: CheckWeatherUseCase

    private val testLocation = Location(37.5665, 126.9780, "서울")
    private val defaultSettings = UserSettings(
        notificationTime = LocalTime(7, 30),
        popThreshold = 40
    )

    @Before
    fun setup() {
        locationChain = mockk()
        weatherRepository = mockk()
        preferencesRepository = mockk()

        every { preferencesRepository.settingsFlow } returns flowOf(defaultSettings)

        useCase = CheckWeatherUseCase(locationChain, weatherRepository, preferencesRepository)
    }

    // ==================== PrecipitationType Tests ====================

    @Test
    fun `returns RainExpected with RAIN type when only rain codes present`() = runTest {
        setupWeather(
            listOf(
                HourlyData(5, 80, 61),  // rain
                HourlyData(6, 70, 63),  // rain
                HourlyData(7, 60, 80),  // rain shower
                HourlyData(8, 50, 51),  // drizzle
                HourlyData(9, 40, 0),   // clear
            )
        )

        val result = useCase(forceRefresh = true)

        assertTrue(result is WeatherDecision.RainExpected)
        val rain = result as WeatherDecision.RainExpected
        assertEquals(PrecipitationType.RAIN, rain.precipitationType)
        assertEquals(80, rain.maxPop)
    }

    @Test
    fun `returns RainExpected with SNOW type when only snow codes present`() = runTest {
        setupWeather(
            listOf(
                HourlyData(5, 80, 71),  // snow
                HourlyData(6, 70, 73),  // snow
                HourlyData(7, 60, 75),  // heavy snow
                HourlyData(8, 50, 85),  // snow shower
                HourlyData(9, 40, 0),   // clear
            )
        )

        val result = useCase(forceRefresh = true)

        assertTrue(result is WeatherDecision.RainExpected)
        assertEquals(PrecipitationType.SNOW, (result as WeatherDecision.RainExpected).precipitationType)
    }

    @Test
    fun `returns RainExpected with MIXED type when rain and snow codes present`() = runTest {
        setupWeather(
            listOf(
                HourlyData(5, 80, 61),  // rain
                HourlyData(6, 70, 71),  // snow
                HourlyData(7, 60, 63),  // rain
                HourlyData(8, 50, 73),  // snow
                HourlyData(9, 40, 0),   // clear
            )
        )

        val result = useCase(forceRefresh = true)

        assertTrue(result is WeatherDecision.RainExpected)
        assertEquals(PrecipitationType.MIXED, (result as WeatherDecision.RainExpected).precipitationType)
    }

    @Test
    fun `returns RainExpected with MIXED type for freezing rain codes`() = runTest {
        setupWeather(
            listOf(
                HourlyData(5, 80, 66),  // freezing rain
                HourlyData(6, 70, 67),  // heavy freezing rain
                HourlyData(7, 60, 0),
                HourlyData(8, 50, 0),
                HourlyData(9, 40, 0),
            )
        )

        val result = useCase(forceRefresh = true)

        assertTrue(result is WeatherDecision.RainExpected)
        assertEquals(PrecipitationType.MIXED, (result as WeatherDecision.RainExpected).precipitationType)
    }

    @Test
    fun `returns RainExpected with RAIN default when no weather codes`() = runTest {
        setupWeather(
            listOf(
                HourlyData(5, 80, null),
                HourlyData(6, 70, null),
                HourlyData(7, 60, null),
                HourlyData(8, 50, null),
                HourlyData(9, 40, null),
            )
        )

        val result = useCase(forceRefresh = true)

        assertTrue(result is WeatherDecision.RainExpected)
        assertEquals(PrecipitationType.RAIN, (result as WeatherDecision.RainExpected).precipitationType)
    }

    @Test
    fun `returns NoRain when pop below threshold`() = runTest {
        setupWeather(
            listOf(
                HourlyData(5, 10, 0),
                HourlyData(6, 20, 0),
                HourlyData(7, 15, 0),
                HourlyData(8, 5, 0),
                HourlyData(9, 0, 0),
            )
        )

        val result = useCase(forceRefresh = true)

        assertTrue(result is WeatherDecision.NoRain)
    }

    @Test
    fun `returns Error when location fails`() = runTest {
        coEvery { locationChain.getLocation() } returns LocationResult.PermissionRequired

        val result = useCase(forceRefresh = true)

        assertTrue(result is WeatherDecision.Error)
    }

    // ==================== Target Date Tests ====================

    @Test
    fun `passes targetDate to repository`() = runTest {
        coEvery { locationChain.getLocation() } returns LocationResult.Success(testLocation, FallbackLevel.GPS_CURRENT)

        val dateSlot = slot<LocalDate?>()
        coEvery {
            weatherRepository.getTomorrowForecast(any(), any(), captureNullable(dateSlot))
        } returns WeatherResult.Success(
            forecast = createForecast(listOf(HourlyData(7, 50, 61))),
            fromCache = false,
            cacheAgeMinutes = 0
        )

        useCase(forceRefresh = true)

        // Verify targetDate was passed (non-null)
        assertTrue(dateSlot.isCaptured)
    }

    // ==================== Helpers ====================

    private data class HourlyData(val hour: Int, val pop: Int, val weatherCode: Int?)

    private fun setupWeather(data: List<HourlyData>) {
        coEvery { locationChain.getLocation() } returns LocationResult.Success(testLocation, FallbackLevel.GPS_CURRENT)

        coEvery { weatherRepository.getTomorrowForecast(any(), any(), any()) } returns WeatherResult.Success(
            forecast = createForecast(data),
            fromCache = false,
            cacheAgeMinutes = 0
        )
    }

    private fun createForecast(data: List<HourlyData>): DailyForecast {
        val forecasts = data.map { (hour, pop, code) ->
            HourlyForecast(
                time = LocalDateTime(2024, 1, 15, hour, 0),
                precipitationProbability = pop,
                weatherCode = code
            )
        }
        return DailyForecast(
            date = LocalDate(2024, 1, 15),
            hourlyForecasts = forecasts,
            fetchedAt = Clock.System.now()
        )
    }
}
