package com.umbrella.domain.model

import kotlinx.datetime.LocalTime

/**
 * 사용자 설정 데이터
 */
data class UserSettings(
    val notificationTime: LocalTime = DEFAULT_NOTIFICATION_TIME,
    val popThreshold: Int = DEFAULT_POP_THRESHOLD,
    val isEnabled: Boolean = true,
    val manualLocation: ManualLocation? = null
) {
    /**
     * PoP 확인 시간 범위 (알림시간 ±2시간)
     */
    val popCheckStartHour: Int
        get() = (notificationTime.hour - 2).coerceAtLeast(0)

    val popCheckEndHour: Int
        get() = (notificationTime.hour + 2).coerceAtMost(23)

    companion object {
        val DEFAULT_NOTIFICATION_TIME = LocalTime(7, 30)
        const val DEFAULT_POP_THRESHOLD = 40

        const val MIN_THRESHOLD = 0
        const val MAX_THRESHOLD = 80
        const val THRESHOLD_STEP = 10

        val MIN_NOTIFICATION_TIME = LocalTime(0, 0)
        val MAX_NOTIFICATION_TIME = LocalTime(23, 59)
    }
}

/**
 * 수동 설정 위치
 */
data class ManualLocation(
    val cityName: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * 한국 주요 도시 목록 (수동 위치 선택용)
 */
object KoreanCities {
    val cities = listOf(
        ManualLocation("서울", 37.5665, 126.9780),
        ManualLocation("부산", 35.1796, 129.0756),
        ManualLocation("인천", 37.4563, 126.7052),
        ManualLocation("대구", 35.8714, 128.6014),
        ManualLocation("대전", 36.3504, 127.3845),
        ManualLocation("광주", 35.1595, 126.8526),
        ManualLocation("울산", 35.5384, 129.3114),
        ManualLocation("세종", 36.4800, 127.2890),
        ManualLocation("수원", 37.2636, 127.0286),
        ManualLocation("창원", 35.2270, 128.6811),
        ManualLocation("고양", 37.6584, 126.8320),
        ManualLocation("용인", 37.2410, 127.1775),
        ManualLocation("청주", 36.6424, 127.4890),
        ManualLocation("전주", 35.8242, 127.1480),
        ManualLocation("천안", 36.8151, 127.1139),
        ManualLocation("제주", 33.4996, 126.5312)
    )

    fun findByName(name: String): ManualLocation? {
        return cities.find { it.cityName == name }
    }
}
