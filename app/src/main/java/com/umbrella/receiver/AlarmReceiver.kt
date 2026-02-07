package com.umbrella.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.data.scheduler.AlarmSchedulerImpl
import com.umbrella.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 알람 수신 → 알림 표시
 *
 * 핵심 로직:
 * 1. 오늘 이미 알림을 표시했으면 무시 (중복 방지)
 * 2. 알림 표시
 * 3. 예약 정보 정리 (DataStore에서 삭제)
 * 4. 알림 표시 날짜 기록
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received: action=${intent.action}")

        val pop = intent.getIntExtra(AlarmSchedulerImpl.EXTRA_POP, 0)

        // 비동기 작업을 위해 pendingResult 사용
        val pendingResult = goAsync()

        scope.launch {
            try {
                // 1. 앱 활성화 상태 확인
                if (!preferencesRepository.isAppEnabled()) {
                    Log.d(TAG, "App disabled, skipping notification")
                    return@launch
                }

                // 2. 오늘 이미 알림을 표시했는지 확인
                if (preferencesRepository.hasNotifiedToday()) {
                    Log.d(TAG, "Already notified today, skipping duplicate")
                    return@launch
                }

                // 3. 알림 표시
                notificationHelper.showRainNotification(pop)
                Log.d(TAG, "Rain notification shown: pop=$pop")

                // 4. 알림 표시 기록
                preferencesRepository.markNotificationShown()

                // 5. 예약 정보 정리
                preferencesRepository.clearScheduledAlarm()
                Log.d(TAG, "Scheduled alarm cleared")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing alarm", e)
            }
        }.invokeOnCompletion {
            // coroutine이 취소되거나 완료되면 항상 finish() 호출
            pendingResult.finish()
        }
    }
}
