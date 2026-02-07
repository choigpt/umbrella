package com.umbrella.data.location

import com.umbrella.domain.model.Location

/**
 * 위치 제공자 인터페이스
 */
interface LocationProvider {

    /**
     * 현재 위치 획득 (타임아웃 포함)
     * @return Location 또는 null (실패 시)
     */
    suspend fun getCurrentLocation(): Location?

    /**
     * 마지막으로 알려진 위치
     */
    suspend fun getLastKnownLocation(): Location?

    /**
     * 위치 권한 확인
     */
    fun hasLocationPermission(): Boolean
}
