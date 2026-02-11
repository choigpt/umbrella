package com.umbrella.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.domain.model.PrecipitationType
import com.umbrella.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager 기반 알림 스케줄러
 *
 * 핵심 원칙:
 * 1. 단일 함수에서 스케줄링 결과 결정 → 실제 등록된 방식만 반환
 * 2. exact 시도 → 실패 시 inexact fallback → 최종 결과 반환
 * 3. DataStore에는 실제 결과만 저장 (targetTime, triggerTime, isExact, bufferApplied)
 * 4. 예외 구분: SecurityException vs 기타 RuntimeException
 */
@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val preferencesRepository: PreferencesRepository
) : NotificationScheduler {

    companion object {
        private const val TAG = "AlarmSchedulerImpl"
        private const val ALARM_REQUEST_CODE = 1001
        const val EXTRA_POP = "extra_pop"
        const val EXTRA_PRECIP_TYPE = "extra_precip_type"

        // 사전확인 알람
        private const val PRE_CHECK_REQUEST_CODE = 1002
        const val ACTION_WEATHER_PRE_CHECK = "com.umbrella.ACTION_WEATHER_PRE_CHECK"
        const val PRE_CHECK_OFFSET_MINUTES = 60

        /**
         * Inexact 알람 버퍼 (분)
         *
         * 목적: Doze 지연 완화 (보장 아님)
         * - setAndAllowWhileIdle()은 Doze maintenance window에서만 실행
         * - Deep Doze에서 최대 15분 지연 → 버퍼 적용해도 5분 늦을 수 있음
         */
        const val INEXACT_BUFFER_MINUTES = 10
    }

    // ==================== Public API ====================

    override suspend fun scheduleNotification(
        time: LocalTime,
        pop: Int,
        precipitationType: PrecipitationType
    ): AlarmScheduleResult {
        val targetMillis = calculateNextTimeMillis(time)
        return scheduleAlarmWithResult(targetMillis, pop, precipitationType)
    }

    /**
     * 특정 시간(epoch millis)에 알람 예약 - 재부팅 후 복구용
     */
    suspend fun scheduleAlarmAt(
        targetMillis: Long,
        pop: Int,
        precipitationType: PrecipitationType = PrecipitationType.RAIN
    ): AlarmScheduleResult {
        return scheduleAlarmWithResult(targetMillis, pop, precipitationType)
    }

    override suspend fun cancelScheduledNotification() {
        val pendingIntent = createPendingIntent(0, PrecipitationType.RAIN)
        alarmManager.cancel(pendingIntent)
        preferencesRepository.clearScheduledAlarm()
        Log.d(TAG, "Alarm cancelled and DataStore cleared")
    }

    override suspend fun getScheduledInfo(): ScheduleInfo? {
        return preferencesRepository.getScheduledAlarmInfo()
    }

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 11 이하에서는 항상 가능
        }
    }

    /**
     * 재부팅/시간 변경 후 알람 복구
     * @return 복구 성공 여부
     */
    suspend fun restoreAlarmIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        if (!preferencesRepository.isAppEnabled()) {
            Log.d(TAG, "App disabled, skipping alarm restore")
            return@withContext false
        }

        val savedInfo = preferencesRepository.getScheduledAlarmInfo()
        if (savedInfo == null) {
            Log.d(TAG, "No scheduled alarm to restore")
            return@withContext false
        }

        val now = Clock.System.now().toEpochMilliseconds()
        if (savedInfo.targetTimeMillis <= now) {
            Log.d(TAG, "Scheduled alarm time already passed, clearing")
            preferencesRepository.clearScheduledAlarm()
            return@withContext false
        }

        // 알람 재등록 (저장된 정보 기반으로 다시 스케줄링)
        val precipType = preferencesRepository.getScheduledPrecipType()
        val result = scheduleAlarmWithResult(savedInfo.targetTimeMillis, savedInfo.pop, precipType)
        when (result) {
            is AlarmScheduleResult.Success -> {
                Log.d(TAG, "Alarm restored: ${result.info.toDiagnosticString()}")
                true
            }
            is AlarmScheduleResult.Failure -> {
                Log.e(TAG, "Alarm restore failed: ${result.reason}")
                false
            }
        }
    }

    // ==================== Core Scheduling Logic ====================

    /**
     * 핵심 스케줄링 함수 - 단일 함수에서 모든 결과 결정
     *
     * 로직:
     * 1. 시간 유효성 검증
     * 2. canScheduleExactAlarms() 체크
     * 3. exact 가능하면 exact 시도 → SecurityException 시 inexact fallback
     * 4. exact 불가능하면 바로 inexact (버퍼 적용)
     * 5. 실제 등록된 결과로 ScheduleInfo 생성 → DataStore 저장 → 반환
     */
    private suspend fun scheduleAlarmWithResult(
        targetMillis: Long,
        pop: Int,
        precipitationType: PrecipitationType = PrecipitationType.RAIN
    ): AlarmScheduleResult = withContext(Dispatchers.IO) {

        // 1. 시간 유효성 검증
        val now = Clock.System.now().toEpochMilliseconds()
        if (targetMillis <= now) {
            Log.w(TAG, "Target time is in the past: $targetMillis <= $now")
            return@withContext AlarmScheduleResult.Failure(FailureReason.INVALID_TIME)
        }

        val pendingIntent = createPendingIntent(pop, precipitationType)

        // 기존 알람 취소
        alarmManager.cancel(pendingIntent)

        // 2. 스케줄링 시도 및 결과 수집
        val scheduleOutcome = attemptScheduling(targetMillis, pendingIntent)

        when (scheduleOutcome) {
            is ScheduleOutcome.Success -> {
                val info = ScheduleInfo(
                    targetTimeMillis = targetMillis,
                    triggerTimeMillis = scheduleOutcome.triggerTimeMillis,
                    isExact = scheduleOutcome.isExact,
                    bufferApplied = scheduleOutcome.bufferApplied,
                    bufferMinutes = if (scheduleOutcome.bufferApplied) INEXACT_BUFFER_MINUTES else 0,
                    pop = pop
                )

                // 3. DataStore에 실제 결과 저장
                preferencesRepository.saveScheduledAlarm(info)
                preferencesRepository.saveScheduledPrecipType(precipitationType)

                Log.d(TAG, "Alarm scheduled successfully:\n${info.toDiagnosticString()}")
                AlarmScheduleResult.Success(info)
            }

            is ScheduleOutcome.Failed -> {
                Log.e(TAG, "Alarm scheduling failed: ${scheduleOutcome.reason}", scheduleOutcome.exception)
                AlarmScheduleResult.Failure(scheduleOutcome.reason, scheduleOutcome.exception)
            }
        }
    }

    /**
     * 실제 AlarmManager 등록 시도
     *
     * @return 등록 결과 (성공 시 실제 등록된 방식 포함)
     */
    private fun attemptScheduling(
        targetMillis: Long,
        pendingIntent: PendingIntent
    ): ScheduleOutcome {

        val canExact = canScheduleExactAlarms()

        return if (canExact) {
            // Exact 알람 시도
            tryExactAlarm(targetMillis, pendingIntent)
        } else {
            // Exact 불가능 → 바로 Inexact (버퍼 적용)
            tryInexactAlarm(targetMillis, pendingIntent)
        }
    }

    /**
     * Exact 알람 시도 → SecurityException 시 Inexact fallback
     */
    private fun tryExactAlarm(
        targetMillis: Long,
        pendingIntent: PendingIntent
    ): ScheduleOutcome {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    targetMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Exact alarm registered at $targetMillis")

            ScheduleOutcome.Success(
                triggerTimeMillis = targetMillis,
                isExact = true,
                bufferApplied = false
            )
        } catch (e: SecurityException) {
            // 권한 철회 등으로 exact 실패 → inexact fallback
            Log.w(TAG, "SecurityException on exact alarm, falling back to inexact", e)
            tryInexactAlarmAfterExactFailed(targetMillis, pendingIntent, e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException on exact alarm", e)
            ScheduleOutcome.Failed(FailureReason.UNKNOWN_ERROR, e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception on exact alarm", e)
            ScheduleOutcome.Failed(FailureReason.UNKNOWN_ERROR, e)
        }
    }

    /**
     * Exact 실패 후 Inexact fallback
     * - 원래 목표 시간에 버퍼 적용
     */
    private fun tryInexactAlarmAfterExactFailed(
        targetMillis: Long,
        pendingIntent: PendingIntent,
        originalException: SecurityException
    ): ScheduleOutcome {
        return try {
            val triggerMillis = calculateBufferedTriggerTime(targetMillis)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Inexact alarm (fallback) registered at $triggerMillis (target was $targetMillis)")

            ScheduleOutcome.Success(
                triggerTimeMillis = triggerMillis,
                isExact = false,
                bufferApplied = true
            )
        } catch (e: Exception) {
            // Inexact도 실패 → 원래 SecurityException을 원인으로 반환
            Log.e(TAG, "Inexact fallback also failed", e)
            ScheduleOutcome.Failed(FailureReason.SECURITY_EXCEPTION, originalException)
        }
    }

    /**
     * Inexact 알람 설정 (버퍼 적용)
     */
    private fun tryInexactAlarm(
        targetMillis: Long,
        pendingIntent: PendingIntent
    ): ScheduleOutcome {
        return try {
            val triggerMillis = calculateBufferedTriggerTime(targetMillis)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Inexact alarm registered at $triggerMillis (target: $targetMillis, buffer: ${INEXACT_BUFFER_MINUTES}min)")

            ScheduleOutcome.Success(
                triggerTimeMillis = triggerMillis,
                isExact = false,
                bufferApplied = true
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException on inexact alarm", e)
            ScheduleOutcome.Failed(FailureReason.SECURITY_EXCEPTION, e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception on inexact alarm", e)
            ScheduleOutcome.Failed(FailureReason.UNKNOWN_ERROR, e)
        }
    }

    // ==================== Helper Methods ====================

    /**
     * 버퍼가 적용된 트리거 시간 계산
     * - 과거 시간 방지: 현재 시간보다 이전이면 현재 시간 + 1분 사용
     */
    private fun calculateBufferedTriggerTime(targetMillis: Long): Long {
        val bufferMillis = INEXACT_BUFFER_MINUTES * 60 * 1000L
        val bufferedTime = targetMillis - bufferMillis

        val now = Clock.System.now().toEpochMilliseconds()
        return if (bufferedTime <= now) {
            // 버퍼 적용하면 과거가 됨 → 최소 1분 후로 설정
            Log.w(TAG, "Buffered time would be in past, using now + 1min")
            now + 60_000L
        } else {
            bufferedTime
        }
    }

    private fun calculateNextTimeMillis(time: LocalTime): Long {
        val now = Clock.System.now()
        val tz = TimeZone.of("Asia/Seoul")
        val todayDateTime = now.toLocalDateTime(tz)
        val today = todayDateTime.date

        // 오늘 해당 시간이 아직 안 지났으면 오늘, 지났으면 내일
        val targetDateTime = time.atDate(today)
        val targetMillis = targetDateTime.toInstant(tz).toEpochMilliseconds()
        val nowMillis = now.toEpochMilliseconds()

        return if (targetMillis > nowMillis) {
            // 오늘 시간이 아직 안 지났으면 오늘로 예약
            targetMillis
        } else {
            // 이미 지났으면 내일로 예약
            val tomorrow = kotlinx.datetime.LocalDate.fromEpochDays(today.toEpochDays() + 1)
            time.atDate(tomorrow).toInstant(tz).toEpochMilliseconds()
        }
    }

    private fun createPendingIntent(pop: Int, precipitationType: PrecipitationType): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.umbrella.ACTION_RAIN_ALARM"
            putExtra(EXTRA_POP, pop)
            putExtra(EXTRA_PRECIP_TYPE, precipitationType.name)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            flags
        )
    }

    // ==================== Pre-Check Alarm (사전확인) ====================

    /**
     * 사전확인 알람 예약 - 알림시간 60분 전에 날씨 체크
     * @param notificationTime 알림 시간
     * @return 예약 성공 여부
     */
    suspend fun schedulePreCheckAlarm(notificationTime: LocalTime): Boolean = withContext(Dispatchers.IO) {
        try {
            val preCheckMillis = calculatePreCheckTimeMillis(notificationTime)
            val now = Clock.System.now().toEpochMilliseconds()

            if (preCheckMillis <= now) {
                Log.d(TAG, "Pre-check time already passed, skipping")
                return@withContext false
            }

            val pendingIntent = createPreCheckPendingIntent()

            // 기존 사전확인 알람 취소
            alarmManager.cancel(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    preCheckMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    preCheckMillis,
                    pendingIntent
                )
            }

            // DataStore에 사전확인 알람 시간 저장 (복구용)
            preferencesRepository.savePreCheckAlarmTime(preCheckMillis)

            Log.d(TAG, "Pre-check alarm scheduled at $preCheckMillis (${PRE_CHECK_OFFSET_MINUTES}min before notification)")
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling pre-check, trying inexact", e)
            try {
                val preCheckMillis = calculatePreCheckTimeMillis(notificationTime)
                val pendingIntent = createPreCheckPendingIntent()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        preCheckMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        preCheckMillis,
                        pendingIntent
                    )
                }
                preferencesRepository.savePreCheckAlarmTime(preCheckMillis)
                true
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule pre-check alarm", e2)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule pre-check alarm", e)
            false
        }
    }

    /**
     * 사전확인 알람 취소
     */
    fun cancelPreCheckAlarm() {
        val pendingIntent = createPreCheckPendingIntent()
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Pre-check alarm cancelled")
    }

    /**
     * 사전확인 알람 복구 (재부팅/시간변경/앱시작 시)
     * @return 복구 성공 여부
     */
    suspend fun restorePreCheckAlarmIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        if (!preferencesRepository.isAppEnabled()) {
            Log.d(TAG, "App disabled, skipping pre-check restore")
            return@withContext false
        }

        return@withContext schedulePreCheckFromSettings()
    }

    /**
     * 사전확인 알람 시간 계산
     * 알림시간에서 PRE_CHECK_OFFSET_MINUTES 전
     * wrap-around 처리: 00:30 알림 → 전날 23:30
     */
    private fun calculatePreCheckTimeMillis(notificationTime: LocalTime): Long {
        val tz = TimeZone.of("Asia/Seoul")
        val now = Clock.System.now()
        val todayDateTime = now.toLocalDateTime(tz)
        val today = todayDateTime.date

        // 알림 시간의 다음 발생 시점 계산
        val notifDateTime = notificationTime.atDate(today)
        val notifMillis = notifDateTime.toInstant(tz).toEpochMilliseconds()
        val nowMillis = now.toEpochMilliseconds()

        val targetNotifMillis = if (notifMillis > nowMillis) {
            notifMillis
        } else {
            val tomorrow = kotlinx.datetime.LocalDate.fromEpochDays(today.toEpochDays() + 1)
            notificationTime.atDate(tomorrow).toInstant(tz).toEpochMilliseconds()
        }

        // 사전확인: 알림시간 - offset
        val preCheckMillis = targetNotifMillis - (PRE_CHECK_OFFSET_MINUTES * 60 * 1000L)

        // 사전확인 시간이 이미 지났으면 (알림이 매우 가까운 경우) 그냥 현재+1분
        return if (preCheckMillis <= nowMillis) {
            nowMillis + 60_000L
        } else {
            preCheckMillis
        }
    }

    private fun createPreCheckPendingIntent(): PendingIntent {
        val intent = Intent(ACTION_WEATHER_PRE_CHECK).apply {
            setPackage(context.packageName)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getBroadcast(
            context,
            PRE_CHECK_REQUEST_CODE,
            intent,
            flags
        )
    }

    /**
     * 현재 설정에서 사전확인 알람 예약
     */
    private suspend fun schedulePreCheckFromSettings(): Boolean {
        val settings = preferencesRepository.settingsFlow.first()
        if (!settings.isEnabled) return false
        return schedulePreCheckAlarm(settings.notificationTime)
    }
}

/**
 * 내부 스케줄링 결과 - AlarmManager 등록 시도의 실제 결과
 */
private sealed class ScheduleOutcome {
    data class Success(
        val triggerTimeMillis: Long,
        val isExact: Boolean,
        val bufferApplied: Boolean
    ) : ScheduleOutcome()

    data class Failed(
        val reason: FailureReason,
        val exception: Throwable? = null
    ) : ScheduleOutcome()
}
