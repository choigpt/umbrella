package com.umbrella.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.umbrella.data.scheduler.AlarmSchedulerImpl
import com.umbrella.domain.usecase.CheckWeatherUseCase
import com.umbrella.domain.usecase.ScheduleNotificationUseCase
import com.umbrella.domain.model.ScheduleResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 주기적 날씨 확인 Worker
 *
 * 실행 시점:
 * - 전날 21:00 (메인 조회)
 * - 당일 04:00 (보정 조회)
 */
@HiltWorker
class WeatherCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkWeatherUseCase: CheckWeatherUseCase,
    private val scheduleNotificationUseCase: ScheduleNotificationUseCase,
    private val alarmScheduler: AlarmSchedulerImpl
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME_EVENING = "weather_check_evening"
        const val WORK_NAME_MORNING = "weather_check_morning"
        const val WORK_NAME_MIDNIGHT = "weather_check_midnight"
        const val WORK_NAME_MIDDAY = "weather_check_midday"
        const val TAG = "WeatherCheckWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            // 날씨 조회
            val decision = checkWeatherUseCase(forceRefresh = true)

            // 알림 예약
            val result = when (scheduleNotificationUseCase(decision)) {
                is ScheduleResult.Scheduled,
                is ScheduleResult.Cancelled -> Result.success()
                is ScheduleResult.Failed -> {
                    // 재시도 (최대 3회)
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }

            // 사전확인 알람 체인 유지
            try {
                alarmScheduler.restorePreCheckAlarmIfNeeded()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule pre-check alarm", e)
            }

            result
        } catch (e: Exception) {
            // 실패해도 사전확인 알람 체인 유지 시도
            try {
                alarmScheduler.restorePreCheckAlarmIfNeeded()
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule pre-check alarm on error", e2)
            }

            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
