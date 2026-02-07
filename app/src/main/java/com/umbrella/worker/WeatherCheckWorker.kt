package com.umbrella.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
    private val scheduleNotificationUseCase: ScheduleNotificationUseCase
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME_EVENING = "weather_check_evening"
        const val WORK_NAME_MORNING = "weather_check_morning"
        const val TAG = "WeatherCheckWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            // 날씨 조회
            val decision = checkWeatherUseCase(forceRefresh = true)

            // 알림 예약
            when (scheduleNotificationUseCase(decision)) {
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
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
