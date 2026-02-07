package com.umbrella.domain.model

/**
 * 앱의 현재 상태를 나타내는 enum.
 * UI에 표시할 상태 코드와 메시지를 포함.
 */
enum class AppStatus(
    val code: String,
    val isError: Boolean = false,
    val requiresAction: Boolean = false
) {
    // === 정상 상태 ===

    /** 정확한 시간에 알림 예약됨 (AlarmManager exact) */
    SCHEDULED_EXACT("SCHED_EXACT"),

    /** 대략적인 시간에 알림 예약됨 (WorkManager, ±15분 오차) */
    SCHEDULED_APPROXIMATE("SCHED_APPROX"),

    /** 내일 비 예보 없음 - 알림 불필요 */
    NO_RAIN_EXPECTED("NO_RAIN"),

    // === 실패 상태 ===

    /** 네트워크 오류로 날씨 조회 실패 */
    FETCH_FAILED_NETWORK("ERR_NETWORK", isError = true),

    /** 위치 획득 실패 */
    FETCH_FAILED_LOCATION("ERR_LOCATION", isError = true, requiresAction = true),

    /** API 오류 */
    FETCH_FAILED_API("ERR_API", isError = true),

    /** 캐시 사용 중 (오프라인 모드) */
    USING_CACHED_DATA("CACHED"),

    // === 권한/설정 필요 ===

    /** 알림 권한 없음 */
    PERMISSION_MISSING_NOTIFICATION("PERM_NOTIF", requiresAction = true),

    /** 위치 권한 없음 + 수동 위치 미설정 */
    PERMISSION_MISSING_LOCATION("PERM_LOC", requiresAction = true),

    /** 정확한 알람 권한 없음 (경고 수준) */
    EXACT_ALARM_UNAVAILABLE("WARN_INEXACT"),

    // === 초기/진행 중 ===

    /** 초기 상태 - 설정 필요 */
    INITIAL("INIT", requiresAction = true),

    /** 날씨 확인 중 */
    CHECKING("CHECKING"),

    /** 알 수 없는 상태 */
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromCode(code: String): AppStatus {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}
