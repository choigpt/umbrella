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
 * 부팅 완료 시:
 * 1. Worker 재등록
 * 2. 예약된 알람 복구
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject
    lateinit var workerScheduler: WorkerScheduler

    @Inject
    lateinit var alarmScheduler: AlarmSchedulerImpl

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()

            scope.launch {
                try {
                    // 1. Worker 재등록
                    workerScheduler.schedulePeriodicWeatherCheck()
                    Log.d(TAG, "Workers rescheduled")

                    // 2. 알람 복구
                    val restored = alarmScheduler.restoreAlarmIfNeeded()
                    Log.d(TAG, "Alarm restore result: $restored")

                } catch (e: Exception) {
                    Log.e(TAG, "Error in boot receiver", e)
                }
            }.invokeOnCompletion {
                // coroutine이 취소되거나 완료되면 항상 finish() 호출
                pendingResult.finish()
            }
        }
    }
}
