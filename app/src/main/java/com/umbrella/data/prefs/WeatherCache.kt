package com.umbrella.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.umbrella.data.api.WeatherResponse
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 날씨 데이터 캐시 관리
 */
@Singleton
class WeatherCache @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    companion object {
        /** 캐시 유효 시간: 6시간 */
        const val CACHE_VALIDITY_HOURS = 6
        private const val CACHE_VALIDITY_MS = CACHE_VALIDITY_HOURS * 60 * 60 * 1000L
    }

    /**
     * 캐시 저장
     */
    suspend fun saveCache(response: WeatherResponse) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.CACHED_FORECAST_JSON] = json.encodeToString(response)
            prefs[UserPreferences.CACHE_SAVED_TIME] = Clock.System.now().toEpochMilliseconds()
        }
    }

    /**
     * 캐시 불러오기 (유효한 경우만)
     * @return Pair<WeatherResponse, 캐시 나이(ms)> 또는 null
     */
    suspend fun getValidCache(): CacheResult? {
        val prefs = dataStore.data.first()

        val jsonString = prefs[UserPreferences.CACHED_FORECAST_JSON] ?: return null
        val savedTime = prefs[UserPreferences.CACHE_SAVED_TIME] ?: return null

        val now = Clock.System.now().toEpochMilliseconds()
        val age = now - savedTime

        // 캐시 만료 체크
        if (age > CACHE_VALIDITY_MS) {
            return null
        }

        return try {
            val response = json.decodeFromString<WeatherResponse>(jsonString)
            CacheResult(response, age)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 캐시 나이를 사람이 읽을 수 있는 형태로 반환
     */
    suspend fun getCacheAgeString(): String? {
        val prefs = dataStore.data.first()
        val savedTime = prefs[UserPreferences.CACHE_SAVED_TIME] ?: return null

        val now = Clock.System.now().toEpochMilliseconds()
        val ageMinutes = (now - savedTime) / 60000

        return when {
            ageMinutes < 60 -> "${ageMinutes}분"
            else -> "${ageMinutes / 60}시간 ${ageMinutes % 60}분"
        }
    }

    /**
     * 캐시 삭제
     */
    suspend fun clearCache() {
        dataStore.edit { prefs ->
            prefs.remove(UserPreferences.CACHED_FORECAST_JSON)
            prefs.remove(UserPreferences.CACHE_SAVED_TIME)
        }
    }
}

data class CacheResult(
    val response: WeatherResponse,
    val ageMillis: Long
) {
    val ageMinutes: Long get() = ageMillis / 60000
    val ageHours: Long get() = ageMillis / 3600000
}
