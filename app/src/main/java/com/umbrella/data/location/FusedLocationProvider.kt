package com.umbrella.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.umbrella.domain.model.Location
import com.umbrella.domain.model.LocationSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationProvider {

    companion object {
        private const val LOCATION_TIMEOUT_MS = 60_000L // 1분
    }

    override suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null

        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val cancellationToken = CancellationTokenSource()

                continuation.invokeOnCancellation {
                    cancellationToken.cancel()
                }

                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_LOW_POWER,
                        cancellationToken.token
                    ).addOnSuccessListener { location ->
                        if (location != null) {
                            val name = getLocationName(location.latitude, location.longitude)
                            continuation.resume(
                                Location(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    name = name,
                                    source = LocationSource.GPS
                                )
                            )
                        } else {
                            continuation.resume(null)
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                } catch (e: SecurityException) {
                    continuation.resume(null)
                }
            }
        }
    }

    override suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        return suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            val name = getLocationName(location.latitude, location.longitude)
                            continuation.resume(
                                Location(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    name = name,
                                    source = LocationSource.CACHED
                                )
                            )
                        } else {
                            continuation.resume(null)
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
        }
    }

    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 좌표 -> 지역명 변환 (Geocoder)
     */
    @Suppress("DEPRECATION")
    private fun getLocationName(lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.KOREA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+ 비동기 방식은 여기서는 생략 (동기 방식 사용)
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                addresses?.firstOrNull()?.let { address ->
                    address.locality ?: address.subAdminArea ?: address.adminArea
                }
            } else {
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                addresses?.firstOrNull()?.let { address ->
                    address.locality ?: address.subAdminArea ?: address.adminArea
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
