package com.umbrella.data.prefs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * DataStore Preferences 키 정의
 */
object UserPreferences {

    // === 사용자 설정 ===

    /** 알림 시간 (분 단위, 0~1440) - 예: 07:30 = 450 */
    val NOTIFICATION_TIME_MINUTES = intPreferencesKey("notification_time_minutes")

    /** 강수확률 임계치 (20~80) */
    val POP_THRESHOLD = intPreferencesKey("pop_threshold")

    /** 알림 활성화 여부 */
    val IS_ENABLED = booleanPreferencesKey("is_enabled")

    // === 수동 위치 설정 ===

    /** 수동 설정 도시명 */
    val MANUAL_CITY_NAME = stringPreferencesKey("manual_city_name")

    /** 수동 설정 위도 */
    val MANUAL_LATITUDE = doublePreferencesKey("manual_latitude")

    /** 수동 설정 경도 */
    val MANUAL_LONGITUDE = doublePreferencesKey("manual_longitude")

    // === 상태 저장 ===

    /** 마지막 상태 코드 */
    val LAST_STATUS_CODE = stringPreferencesKey("last_status_code")

    // === 알람 예약 정보 (실제 스케줄 결과 기반) ===

    /** 목표 알림 시간 - 사용자가 원하는 시간 (epoch millis) */
    val SCHEDULED_ALARM_TARGET_TIME = longPreferencesKey("scheduled_alarm_target_time")

    /** 실제 트리거 시간 - AlarmManager에 등록된 시간 (epoch millis) */
    val SCHEDULED_ALARM_TRIGGER_TIME = longPreferencesKey("scheduled_alarm_trigger_time")

    /** 정확한 알람 여부 - 실제로 exact로 등록되었는지 */
    val SCHEDULED_ALARM_IS_EXACT = booleanPreferencesKey("scheduled_alarm_is_exact")

    /** 버퍼 적용 여부 - inexact일 때 10분 앞당김 적용됨 */
    val SCHEDULED_ALARM_BUFFER_APPLIED = booleanPreferencesKey("scheduled_alarm_buffer_applied")

    /** 적용된 버퍼 시간 (분) */
    val SCHEDULED_ALARM_BUFFER_MINUTES = intPreferencesKey("scheduled_alarm_buffer_minutes")

    /** 예약된 알림의 PoP */
    val SCHEDULED_ALARM_POP = intPreferencesKey("scheduled_alarm_pop")

    // === Legacy (마이그레이션 후 제거 예정) ===
    @Deprecated("Use SCHEDULED_ALARM_TARGET_TIME instead")
    val SCHEDULED_ALARM_TIME = longPreferencesKey("scheduled_alarm_time")

    /** 마지막 계산된 PoP */
    val LAST_CALCULATED_POP = intPreferencesKey("last_calculated_pop")

    /** 마지막 위치명 */
    val LAST_LOCATION_NAME = stringPreferencesKey("last_location_name")

    /** 마지막 업데이트 시간 (epoch millis) */
    val LAST_UPDATE_TIME = longPreferencesKey("last_update_time")

    // === 중복 알림 방지 ===

    /** 마지막으로 알림을 표시한 날짜 (yyyy-MM-dd) */
    val LAST_NOTIFICATION_DATE = stringPreferencesKey("last_notification_date")

    // === 캐시 ===

    /** 캐시된 예보 데이터 (JSON) */
    val CACHED_FORECAST_JSON = stringPreferencesKey("cached_forecast_json")

    /** 캐시 저장 시간 (epoch millis) */
    val CACHE_SAVED_TIME = longPreferencesKey("cache_saved_time")

    // === 기타 ===

    /** 첫 실행 완료 여부 */
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

    /** 연속 실패 횟수 */
    val CONSECUTIVE_FAILURES = intPreferencesKey("consecutive_failures")

    /** 마지막 실패 날짜 (yyyy-MM-dd) - 실패 알림 쿨다운용 */
    val LAST_FAILURE_DATE = stringPreferencesKey("last_failure_date")
}
