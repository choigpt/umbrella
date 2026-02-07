package com.umbrella.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.umbrella.data.scheduler.AlarmSchedulerImpl
import com.umbrella.worker.WorkerScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 시간/타임존 변경 시 알람 재등록
 *
 * 처리하는 인텐트:
 * - ACTION_TIME_CHANGED: 수동 시간 변경
 * - ACTION_TIMEZONE_CHANGED: 타임존 변경
 */
@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TimeChangeReceiver"
    }

    @Inject
    lateinit var workerScheduler: WorkerScheduler

    @Inject
    lateinit var alarmScheduler: AlarmSchedulerImpl

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Time change received: $action")

        // ACTION_DATE_CHANGED는 deprecated이지만 일부 기기에서 여전히 발생
        if (action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            val pendingResult = goAsync()

            scope.launch {
                try {
                    // Worker 재등록 (시간 기준이 바뀌었으므로)
                    workerScheduler.schedulePeriodicWeatherCheck()
                    Log.d(TAG, "Workers rescheduled after time change")

                    // 알람 재등록 (epoch millis는 그대로지만, 시스템 시간이 바뀌면 재등록 필요)
                    val restored = alarmScheduler.restoreAlarmIfNeeded()
                    Log.d(TAG, "Alarm restore after time change: $restored")

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling time change", e)
                }
            }.invokeOnCompletion {
                // coroutine이 취소되거나 완료되면 항상 finish() 호출
                pendingResult.finish()
            }
        }
    }
}
