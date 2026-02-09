package com.umbrella.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.umbrella.data.scheduler.ScheduleInfo
import com.umbrella.domain.model.AppStatus
import com.umbrella.domain.model.ManualLocation
import com.umbrella.domain.model.StatusInfo
import com.umbrella.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    // ==================== 사용자 설정 ====================

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { prefs ->
        val timeMinutes = prefs[UserPreferences.NOTIFICATION_TIME_MINUTES]
            ?: (7 * 60 + 30) // 기본 07:30

        val manualLocation = prefs[UserPreferences.MANUAL_CITY_NAME]?.let { name ->
            val lat = prefs[UserPreferences.MANUAL_LATITUDE] ?: return@let null
            val lon = prefs[UserPreferences.MANUAL_LONGITUDE] ?: return@let null
            ManualLocation(name, lat, lon)
        }

        UserSettings(
            notificationTime = LocalTime(timeMinutes / 60, timeMinutes % 60),
            popThreshold = prefs[UserPreferences.POP_THRESHOLD]
                ?: UserSettings.DEFAULT_POP_THRESHOLD,
            isEnabled = prefs[UserPreferences.IS_ENABLED] ?: true,
            manualLocation = manualLocation
        )
    }

    suspend fun updateNotificationTime(time: LocalTime) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.NOTIFICATION_TIME_MINUTES] = time.hour * 60 + time.minute
        }
    }

    suspend fun updatePopThreshold(threshold: Int) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.POP_THRESHOLD] = threshold.coerceIn(
                UserSettings.MIN_THRESHOLD,
                UserSettings.MAX_THRESHOLD
            )
        }
    }

    suspend fun updateEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.IS_ENABLED] = enabled
        }
    }

    suspend fun updateManualLocation(location: ManualLocation?) {
        dataStore.edit { prefs ->
            if (location != null) {
                prefs[UserPreferences.MANUAL_CITY_NAME] = location.cityName
                prefs[UserPreferences.MANUAL_LATITUDE] = location.latitude
                prefs[UserPreferences.MANUAL_LONGITUDE] = location.longitude
            } else {
                prefs.remove(UserPreferences.MANUAL_CITY_NAME)
                prefs.remove(UserPreferences.MANUAL_LATITUDE)
                prefs.remove(UserPreferences.MANUAL_LONGITUDE)
            }
        }
    }

    // ==================== 상태 정보 ====================

    val statusFlow: Flow<StatusInfo> = dataStore.data.map { prefs ->
        val statusCode = prefs[UserPreferences.LAST_STATUS_CODE]
        val status = statusCode?.let { AppStatus.fromCode(it) } ?: AppStatus.INITIAL

        // 새로운 키 우선, 없으면 레거시 키 사용
        val targetTimeMillis = prefs[UserPreferences.SCHEDULED_ALARM_TARGET_TIME]
            ?: @Suppress("DEPRECATION") prefs[UserPreferences.SCHEDULED_ALARM_TIME]

        val timeMinutes = prefs[UserPreferences.NOTIFICATION_TIME_MINUTES] ?: (7 * 60 + 30)
        val notificationTime = LocalTime(timeMinutes / 60, timeMinutes % 60)
        val threshold = prefs[UserPreferences.POP_THRESHOLD] ?: UserSettings.DEFAULT_POP_THRESHOLD

        StatusInfo(
            status = status,
            scheduledTime = if (targetTimeMillis != null) notificationTime else null,
            pop = prefs[UserPreferences.LAST_CALCULATED_POP],
            threshold = threshold,
            locationName = prefs[UserPreferences.LAST_LOCATION_NAME],
            lastUpdateTime = prefs[UserPreferences.LAST_UPDATE_TIME]?.let {
                Instant.fromEpochMilliseconds(it)
            }
        )
    }

    suspend fun updateStatus(
        status: AppStatus,
        pop: Int? = null,
        locationName: String? = null
    ) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.LAST_STATUS_CODE] = status.code
            prefs[UserPreferences.LAST_UPDATE_TIME] = Clock.System.now().toEpochMilliseconds()

            pop?.let {
                prefs[UserPreferences.LAST_CALCULATED_POP] = it
            }
            locationName?.let { prefs[UserPreferences.LAST_LOCATION_NAME] = it }
        }
    }

    // ==================== 알람 예약 정보 (실제 결과 기반) ====================

    /**
     * 알람 예약 정보 저장 - 실제 스케줄링 결과 기반
     *
     * @param info AlarmSchedulerImpl에서 반환된 실제 스케줄링 결과
     */
    suspend fun saveScheduledAlarm(info: ScheduleInfo) {
        dataStore.edit { prefs ->
            prefs[UserPreferences.SCHEDULED_ALARM_TARGET_TIME] = info.targetTimeMillis
            prefs[UserPreferences.SCHEDULED_ALARM_TRIGGER_TIME] = info.triggerTimeMillis
            prefs[UserPreferences.SCHEDULED_ALARM_IS_EXACT] = info.isExact
            prefs[UserPreferences.SCHEDULED_ALARM_BUFFER_APPLIED] = info.bufferApplied
            prefs[UserPreferences.SCHEDULED_ALARM_BUFFER_MINUTES] = info.bufferMinutes
            prefs[UserPreferences.SCHEDULED_ALARM_POP] = info.pop

            // 레거시 키도 업데이트 (호환성)
            @Suppress("DEPRECATION")
            prefs[UserPreferences.SCHEDULED_ALARM_TIME] = info.targetTimeMillis
        }
    }

    /**
     * 예약된 알람 정보 조회 (재부팅/시간 변경 후 복구용)
     *
     * @return ScheduleInfo 또는 null (예약 없거나 이미 지난 경우)
     */
    suspend fun getScheduledAlarmInfo(): ScheduleInfo? {
        val prefs = dataStore.data.first()

        // 새로운 키 우선
        val targetTimeMillis = prefs[UserPreferences.SCHEDULED_ALARM_TARGET_TIME]

        // 새 키가 없으면 레거시 키 시도 (마이그레이션)
        if (targetTimeMillis == null) {
            @Suppress("DEPRECATION")
            val legacyTime = prefs[UserPreferences.SCHEDULED_ALARM_TIME] ?: return null
            val legacyPop = prefs[UserPreferences.SCHEDULED_ALARM_POP] ?: 0

            // 레거시 데이터는 exact 여부를 알 수 없으므로 기본값 사용
            if (legacyTime <= Clock.System.now().toEpochMilliseconds()) {
                return null
            }

            return ScheduleInfo(
                targetTimeMillis = legacyTime,
                triggerTimeMillis = legacyTime, // 레거시는 동일하게 취급
                isExact = true, // 레거시는 알 수 없으므로 true 가정
                bufferApplied = false,
                bufferMinutes = 0,
                pop = legacyPop
            )
        }

        // 이미 지난 시간이면 null
        if (targetTimeMillis <= Clock.System.now().toEpochMilliseconds()) {
            return null
        }

        val triggerTimeMillis = prefs[UserPreferences.SCHEDULED_ALARM_TRIGGER_TIME] ?: targetTimeMillis
        val isExact = prefs[UserPreferences.SCHEDULED_ALARM_IS_EXACT] ?: true
        val bufferApplied = prefs[UserPreferences.SCHEDULED_ALARM_BUFFER_APPLIED] ?: false
        val bufferMinutes = prefs[UserPreferences.SCHEDULED_ALARM_BUFFER_MINUTES] ?: 0
        val pop = prefs[UserPreferences.SCHEDULED_ALARM_POP] ?: 0

        return ScheduleInfo(
            targetTimeMillis = targetTimeMillis,
            triggerTimeMillis = triggerTimeMillis,
            isExact = isExact,
            bufferApplied = bufferApplied,
            bufferMinutes = bufferMinutes,
            pop = pop
        )
    }

    /**
     * 알람 예약 정보 삭제 (알림 표시 후 호출)
     */
    suspend fun clearScheduledAlarm() {
        dataStore.edit { prefs ->
            // 새로운 키 삭제
            prefs.remove(UserPreferences.SCHEDULED_ALARM_TARGET_TIME)
            prefs.remove(UserPreferences.SCHEDULED_ALARM_TRIGGER_TIME)
            prefs.remove(UserPreferences.SCHEDULED_ALARM_IS_EXACT)
            prefs.remove(UserPreferences.SCHEDULED_ALARM_BUFFER_APPLIED)
            prefs.remove(UserPreferences.SCHEDULED_ALARM_BUFFER_MINUTES)
            prefs.remove(UserPreferences.SCHEDULED_ALARM_POP)

            // 레거시 키도 삭제
            @Suppress("DEPRECATION")
            prefs.remove(UserPreferences.SCHEDULED_ALARM_TIME)

            prefs[UserPreferences.LAST_STATUS_CODE] = AppStatus.INITIAL.code
        }
    }

    // ==================== 중복 알림 방지 ====================

    /**
     * 오늘 이미 알림을 표시했는지 확인
     */
    suspend fun hasNotifiedToday(): Boolean {
        val prefs = dataStore.data.first()
        val lastDate = prefs[UserPreferences.LAST_NOTIFICATION_DATE] ?: return false
        val today = getTodayDateString()
        return lastDate == today
    }

    /**
     * 알림 표시 기록
     */
    suspend fun markNotificationShown() {
        dataStore.edit { prefs ->
            prefs[UserPreferences.LAST_NOTIFICATION_DATE] = getTodayDateString()
        }
    }

    private fun getTodayDateString(): String {
        val now = Clock.System.now()
        val localDate = now.toLocalDateTime(TimeZone.of("Asia/Seoul")).date
        return "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"
    }

    // ==================== 실패 카운터 ====================

    /**
     * 실패 횟수 증가 (같은 날에만 카운트)
     * @return 현재 실패 횟수
     */
    suspend fun incrementFailureCount(): Int {
        val today = getTodayDateString()
        var count = 0

        dataStore.edit { prefs ->
            val lastFailureDate = prefs[UserPreferences.LAST_FAILURE_DATE]

            // 날짜가 바뀌면 카운터 리셋
            if (lastFailureDate != today) {
                prefs[UserPreferences.CONSECUTIVE_FAILURES] = 1
                prefs[UserPreferences.LAST_FAILURE_DATE] = today
                count = 1
            } else {
                count = (prefs[UserPreferences.CONSECUTIVE_FAILURES] ?: 0) + 1
                prefs[UserPreferences.CONSECUTIVE_FAILURES] = count
            }
        }
        return count
    }

    suspend fun resetFailureCount() {
        dataStore.edit { prefs ->
            prefs[UserPreferences.CONSECUTIVE_FAILURES] = 0
        }
    }

    suspend fun getFailureCount(): Int {
        return dataStore.data.first()[UserPreferences.CONSECUTIVE_FAILURES] ?: 0
    }

    // ==================== 온보딩 ====================

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[UserPreferences.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun completeOnboarding() {
        dataStore.edit { prefs ->
            prefs[UserPreferences.ONBOARDING_COMPLETED] = true
        }
    }

    // ==================== 앱 활성화 상태 ====================

    suspend fun isAppEnabled(): Boolean {
        return dataStore.data.first()[UserPreferences.IS_ENABLED] ?: true
    }

    // ==================== 진단 정보 ====================

    /**
     * DataStore 내 모든 값을 텍스트로 덤프 (디버깅용)
     *
     * UI 표시 규칙:
     * - 기본 UI (상태 카드): targetTime만 표시
     * - 진단 정보: targetTime, triggerTime, isExact, bufferApplied 모두 표시
     */
    suspend fun getDiagnosticInfo(): String {
        val prefs = dataStore.data.first()
        val sb = StringBuilder()

        sb.appendLine("=== Umbrella 진단 정보 ===")
        sb.appendLine()

        // 알림 설정
        sb.appendLine("[알림 설정]")
        val timeMinutes = prefs[UserPreferences.NOTIFICATION_TIME_MINUTES] ?: (7 * 60 + 30)
        sb.appendLine("알림 시간: ${timeMinutes / 60}:${(timeMinutes % 60).toString().padStart(2, '0')}")
        sb.appendLine("임계치: ${prefs[UserPreferences.POP_THRESHOLD] ?: 40}%")
        sb.appendLine("활성화: ${prefs[UserPreferences.IS_ENABLED] ?: true}")
        sb.appendLine()

        // 위치 설정
        sb.appendLine("[위치 설정]")
        val cityName = prefs[UserPreferences.MANUAL_CITY_NAME]
        if (cityName != null) {
            sb.appendLine("수동 위치: $cityName")
            sb.appendLine("위도: ${prefs[UserPreferences.MANUAL_LATITUDE]}")
            sb.appendLine("경도: ${prefs[UserPreferences.MANUAL_LONGITUDE]}")
        } else {
            sb.appendLine("위치: GPS (자동)")
        }
        sb.appendLine()

        // 알람 예약 상태 (핵심 진단 정보)
        sb.appendLine("[알람 예약 상태]")
        val targetTime = prefs[UserPreferences.SCHEDULED_ALARM_TARGET_TIME]
        if (targetTime != null) {
            val triggerTime = prefs[UserPreferences.SCHEDULED_ALARM_TRIGGER_TIME] ?: targetTime
            val isExact = prefs[UserPreferences.SCHEDULED_ALARM_IS_EXACT] ?: true
            val bufferApplied = prefs[UserPreferences.SCHEDULED_ALARM_BUFFER_APPLIED] ?: false
            val bufferMinutes = prefs[UserPreferences.SCHEDULED_ALARM_BUFFER_MINUTES] ?: 0

            sb.appendLine("목표 시간 (UI표시): ${formatMillis(targetTime)}")
            sb.appendLine("트리거 시간 (실제): ${formatMillis(triggerTime)}")
            sb.appendLine("알람 유형: ${if (isExact) "정확 (Exact)" else "비정확 (Inexact)"}")
            sb.appendLine("버퍼 적용: ${if (bufferApplied) "예 (${bufferMinutes}분 앞당김)" else "아니오"}")
            sb.appendLine("강수확률: ${prefs[UserPreferences.SCHEDULED_ALARM_POP] ?: 0}%")

            if (!isExact) {
                sb.appendLine()
                sb.appendLine("⚠️ Inexact 알람은 최대 15분 지연될 수 있습니다")
            }
        } else {
            sb.appendLine("예약 없음")
        }
        sb.appendLine()

        // 마지막 상태
        sb.appendLine("[마지막 상태]")
        sb.appendLine("상태 코드: ${prefs[UserPreferences.LAST_STATUS_CODE] ?: "없음"}")
        sb.appendLine("마지막 POP: ${prefs[UserPreferences.LAST_CALCULATED_POP] ?: "없음"}")
        sb.appendLine("마지막 위치: ${prefs[UserPreferences.LAST_LOCATION_NAME] ?: "없음"}")
        val lastUpdate = prefs[UserPreferences.LAST_UPDATE_TIME]
        if (lastUpdate != null) {
            sb.appendLine("마지막 업데이트: ${formatMillis(lastUpdate)}")
        }
        sb.appendLine()

        // 알림 기록
        sb.appendLine("[알림 기록]")
        sb.appendLine("마지막 알림 날짜: ${prefs[UserPreferences.LAST_NOTIFICATION_DATE] ?: "없음"}")
        sb.appendLine("연속 실패: ${prefs[UserPreferences.CONSECUTIVE_FAILURES] ?: 0}")
        sb.appendLine("마지막 실패 날짜: ${prefs[UserPreferences.LAST_FAILURE_DATE] ?: "없음"}")

        return sb.toString()
    }

    private fun formatMillis(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val localDateTime = instant.toLocalDateTime(TimeZone.of("Asia/Seoul"))
        return "${localDateTime.date} ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
    }
}
