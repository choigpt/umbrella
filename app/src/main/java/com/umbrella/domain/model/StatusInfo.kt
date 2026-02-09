package com.umbrella.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime

/**
 * 앱 상태와 관련 메타데이터를 담는 데이터 클래스.
 * UI에 표시할 모든 정보를 포함.
 */
data class StatusInfo(
    val status: AppStatus,
    val scheduledTime: LocalTime? = null,
    val pop: Int? = null,
    val threshold: Int? = null,
    val locationName: String? = null,
    val lastUpdateTime: Instant? = null,
    val nextRetryTime: Instant? = null,
    val cacheAge: String? = null,
    val errorMessage: String? = null
) {
    private fun formatTime(time: LocalTime?): String {
        if (time == null) return "07:30"
        val hour = time.hour
        val minute = time.minute.toString().padStart(2, '0')
        val amPm = if (hour < 12) "오전" else "오후"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return "$amPm ${displayHour}:${minute}"
    }

    /**
     * 사용자에게 표시할 메인 메시지
     */
    val userMessage: String
        get() = when (status) {
            AppStatus.SCHEDULED_EXACT,
            AppStatus.SCHEDULED_APPROXIMATE -> "알림 예약됨"

            AppStatus.NO_RAIN_EXPECTED -> "비 소식 없음"

            AppStatus.FETCH_FAILED_NETWORK -> "날씨 정보 조회 실패"
            AppStatus.FETCH_FAILED_LOCATION -> "위치를 가져올 수 없음"
            AppStatus.FETCH_FAILED_API -> "날씨 서비스 오류"
            AppStatus.USING_CACHED_DATA -> "캐시된 데이터 사용 중"

            AppStatus.PERMISSION_MISSING_NOTIFICATION -> "알림 권한 필요"
            AppStatus.PERMISSION_MISSING_LOCATION -> "위치 설정 필요"
            AppStatus.EXACT_ALARM_UNAVAILABLE -> "정확한 시간 알림 불가"

            AppStatus.INITIAL -> "설정을 완료하세요"
            AppStatus.CHECKING -> "날씨 확인 중..."
            AppStatus.UNKNOWN -> "상태 확인 필요"
        }

    /**
     * 사용자에게 표시할 상세 메시지
     */
    val detailMessage: String?
        get() = when (status) {
            AppStatus.SCHEDULED_EXACT -> {
                "${formatTime(scheduledTime)}에 알림이 울립니다 (강수확률 ${pop ?: 0}%)"
            }

            AppStatus.SCHEDULED_APPROXIMATE -> {
                "${formatTime(scheduledTime)} 전후로 알림이 울립니다 (±15분 오차 가능)"
            }

            AppStatus.NO_RAIN_EXPECTED -> {
                "강수확률 ${pop ?: 0}%로 임계치(${threshold ?: 40}%) 미만입니다"
            }

            AppStatus.FETCH_FAILED_NETWORK -> {
                val cacheInfo = cacheAge?.let { "마지막 데이터: $it" } ?: ""
                "네트워크 연결을 확인하세요. $cacheInfo"
            }

            AppStatus.FETCH_FAILED_LOCATION -> {
                "위치 권한을 확인하거나 수동으로 지역을 설정하세요"
            }

            AppStatus.FETCH_FAILED_API -> {
                nextRetryTime?.let { "잠시 후 다시 시도됩니다" }
                    ?: "잠시 후 다시 시도해주세요"
            }

            AppStatus.USING_CACHED_DATA -> {
                cacheAge?.let { "$it 전 데이터를 사용 중입니다" }
            }

            AppStatus.PERMISSION_MISSING_NOTIFICATION -> {
                "알림을 받으려면 권한을 허용해주세요"
            }

            AppStatus.PERMISSION_MISSING_LOCATION -> {
                "위치 권한을 허용하거나 지역을 수동 설정하세요"
            }

            AppStatus.EXACT_ALARM_UNAVAILABLE -> {
                "시스템 설정에서 '정확한 알람' 권한을 허용하면 정시 알림이 가능합니다"
            }

            AppStatus.INITIAL -> {
                "알림 시간과 강수확률 임계치를 설정하세요"
            }

            AppStatus.CHECKING -> null

            AppStatus.UNKNOWN -> errorMessage
        }

    companion object {
        val INITIAL = StatusInfo(AppStatus.INITIAL)

        fun checking() = StatusInfo(AppStatus.CHECKING)

        fun scheduled(
            isExact: Boolean,
            time: LocalTime,
            pop: Int,
            locationName: String?,
            lastUpdate: Instant
        ) = StatusInfo(
            status = if (isExact) AppStatus.SCHEDULED_EXACT else AppStatus.SCHEDULED_APPROXIMATE,
            scheduledTime = time,
            pop = pop,
            locationName = locationName,
            lastUpdateTime = lastUpdate
        )

        fun noRain(
            pop: Int,
            threshold: Int,
            locationName: String?,
            lastUpdate: Instant
        ) = StatusInfo(
            status = AppStatus.NO_RAIN_EXPECTED,
            pop = pop,
            threshold = threshold,
            locationName = locationName,
            lastUpdateTime = lastUpdate
        )

        fun error(
            status: AppStatus,
            message: String? = null,
            cacheAge: String? = null
        ) = StatusInfo(
            status = status,
            errorMessage = message,
            cacheAge = cacheAge
        )
    }
}
