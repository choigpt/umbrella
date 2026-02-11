package com.umbrella.worker

import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager 스케줄링 헬퍼
 *
 * 스케줄 (목표 시간, 정확하지 않음):
 * - 저녁 조회 (21:00 전후): 내일 예보 확인 → 알림 예약
 * - 아침 조회 (04:00 전후): 예보 갱신, 필요시 알림 재예약
 *
 * ⚠️ 중요 제약사항:
 * - PeriodicWorkRequest는 정확한 시간을 보장하지 않음
 * - initialDelay로 첫 실행 시간을 유도하지만, 이후 실행은 시스템이 결정
 * - flexTimeInterval (30분)은 목표 시간 전 30분 윈도우를 의미
 * - Doze/배터리 최적화 상태에서는 수 시간 지연될 수 있음
 * - 핵심 알림 시간은 AlarmManager (AlarmSchedulerImpl)가 담당
 */
@Singleton
class WorkerScheduler @Inject constructor(
    private val workManager: WorkManager
) {

    companion object {
        private const val TAG = "WorkerScheduler"
        private const val MIDNIGHT_HOUR = 0   // 00:00
        private const val MORNING_HOUR = 6    // 06:00
        private const val MIDDAY_HOUR = 12    // 12:00
        private const val EVENING_HOUR = 21   // 21:00

        // flex window: 목표 시간 ±30분 (총 1시간 윈도우)
        private const val FLEX_INTERVAL_MINUTES = 30L
    }

    /**
     * 주기적 날씨 확인 Worker 등록 (4회/일)
     * 00:00, 06:00, 12:00, 21:00
     */
    fun schedulePeriodicWeatherCheck() {
        scheduleMidnightCheck()
        scheduleMorningCheck()
        scheduleMiddayCheck()
        scheduleEveningCheck()
        Log.d(TAG, "Periodic weather checks scheduled (4x/day)")
    }

    /**
     * 자정 체크 (00:00) - 예보 수집 + 사전확인 알람 갱신
     */
    private fun scheduleMidnightCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val delay = calculateDelayUntil(MIDNIGHT_HOUR, 0)

        val workRequest = PeriodicWorkRequestBuilder<WeatherCheckWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = FLEX_INTERVAL_MINUTES,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WeatherCheckWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WeatherCheckWorker.WORK_NAME_MIDNIGHT,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.d(TAG, "Midnight check scheduled, delay=${delay}ms")
    }

    /**
     * 아침 체크 (06:00) - 예보 갱신 + 사전확인 알람 갱신
     */
    private fun scheduleMorningCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val delay = calculateDelayUntil(MORNING_HOUR, 0)

        val workRequest = PeriodicWorkRequestBuilder<WeatherCheckWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = FLEX_INTERVAL_MINUTES,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WeatherCheckWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WeatherCheckWorker.WORK_NAME_MORNING,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.d(TAG, "Morning check scheduled, delay=${delay}ms")
    }

    /**
     * 점심 체크 (12:00) - 내일 예보 수집
     */
    private fun scheduleMiddayCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val delay = calculateDelayUntil(MIDDAY_HOUR, 0)

        val workRequest = PeriodicWorkRequestBuilder<WeatherCheckWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = FLEX_INTERVAL_MINUTES,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WeatherCheckWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WeatherCheckWorker.WORK_NAME_MIDDAY,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.d(TAG, "Midday check scheduled, delay=${delay}ms")
    }

    /**
     * 저녁 체크 (21:00) - 내일 예보 수집 + 알람 예약
     */
    private fun scheduleEveningCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val delay = calculateDelayUntil(EVENING_HOUR, 0)

        val workRequest = PeriodicWorkRequestBuilder<WeatherCheckWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = FLEX_INTERVAL_MINUTES,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WeatherCheckWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WeatherCheckWorker.WORK_NAME_EVENING,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.d(TAG, "Evening check scheduled, delay=${delay}ms")
    }

    /**
     * 즉시 날씨 확인 실행 (설정 변경 시 호출)
     */
    fun runImmediateWeatherCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<WeatherCheckWorker>()
            .setConstraints(constraints)
            .addTag(WeatherCheckWorker.TAG)
            .build()

        workManager.enqueue(workRequest)
        Log.d(TAG, "Immediate weather check enqueued")
    }

    /**
     * 모든 예약된 작업 취소
     */
    fun cancelAllWork() {
        workManager.cancelUniqueWork(WeatherCheckWorker.WORK_NAME_MIDNIGHT)
        workManager.cancelUniqueWork(WeatherCheckWorker.WORK_NAME_MORNING)
        workManager.cancelUniqueWork(WeatherCheckWorker.WORK_NAME_MIDDAY)
        workManager.cancelUniqueWork(WeatherCheckWorker.WORK_NAME_EVENING)
        Log.d(TAG, "All weather checks cancelled")
    }

    /**
     * 특정 시간까지의 지연 시간 계산 (밀리초)
     */
    private fun calculateDelayUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 이미 지났으면 다음날로
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return target.timeInMillis - now.timeInMillis
    }
}
