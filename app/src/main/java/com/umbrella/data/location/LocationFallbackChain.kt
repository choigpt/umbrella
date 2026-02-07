package com.umbrella.data.location

import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.domain.model.Location
import com.umbrella.domain.model.LocationSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치 획득 Fallback 체인
 *
 * 1순위: FusedLocation 현재 위치 (1분 타임아웃)
 * 2순위: 캐시된 마지막 위치
 * 3순위: 사용자 수동 설정 도시
 */
@Singleton
class LocationFallbackChain @Inject constructor(
    private val fusedLocationProvider: FusedLocationProvider,
    private val preferencesRepository: PreferencesRepository
) {

    /**
     * Fallback 체인을 통해 위치 획득
     * @return Pair<Location, 사용된 Fallback 단계>
     */
    suspend fun getLocation(): LocationResult {
        // 1순위: 현재 위치
        fusedLocationProvider.getCurrentLocation()?.let {
            return LocationResult.Success(it, FallbackLevel.GPS_CURRENT)
        }

        // 2순위: 캐시된 마지막 위치
        fusedLocationProvider.getLastKnownLocation()?.let {
            return LocationResult.Success(it, FallbackLevel.GPS_CACHED)
        }

        // 3순위: 수동 설정 위치
        val settings = preferencesRepository.settingsFlow.first()
        settings.manualLocation?.let { manual ->
            val location = Location(
                latitude = manual.latitude,
                longitude = manual.longitude,
                name = manual.cityName,
                source = LocationSource.MANUAL
            )
            return LocationResult.Success(location, FallbackLevel.MANUAL)
        }

        // 모든 방법 실패
        return if (!fusedLocationProvider.hasLocationPermission()) {
            LocationResult.PermissionRequired
        } else {
            LocationResult.ManualSettingRequired
        }
    }
}

sealed class LocationResult {
    data class Success(
        val location: Location,
        val fallbackLevel: FallbackLevel
    ) : LocationResult()

    data object PermissionRequired : LocationResult()
    data object ManualSettingRequired : LocationResult()
}

enum class FallbackLevel {
    GPS_CURRENT,  // 1순위: 현재 GPS
    GPS_CACHED,   // 2순위: 캐시된 GPS
    MANUAL        // 3순위: 수동 설정
}
