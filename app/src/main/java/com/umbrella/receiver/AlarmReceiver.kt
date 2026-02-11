package com.umbrella.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.data.scheduler.AlarmSchedulerImpl
import com.umbrella.domain.model.PrecipitationType
import com.umbrella.notification.NotificationHelper
import com.umbrella.presentation.alert.AlarmAlertActivity
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
        val receivedAt = System.currentTimeMillis()
        Log.d(TAG, "Alarm received: action=${intent.action}, time=$receivedAt")

        val pop = intent.getIntExtra(AlarmSchedulerImpl.EXTRA_POP, 0)
        val precipTypeName = intent.getStringExtra(AlarmSchedulerImpl.EXTRA_PRECIP_TYPE)
        val precipType = try {
            precipTypeName?.let { PrecipitationType.valueOf(it) } ?: PrecipitationType.RAIN
        } catch (_: IllegalArgumentException) {
            PrecipitationType.RAIN
        }
        Log.d(TAG, "  pop=$pop, precipType=$precipType")

        // 비동기 작업을 위해 pendingResult 사용
        val pendingResult = goAsync()

        scope.launch {
            try {
                // 1. 앱 활성화 상태 확인
                if (!preferencesRepository.isAppEnabled()) {
                    Log.d(TAG, "App disabled, skipping notification")
                    return@launch
                }

                // TODO: 테스트 완료 후 복구할 것
                // 2. 오늘 이미 알림을 표시했는지 확인
                // if (preferencesRepository.hasNotifiedToday()) {
                //     Log.d(TAG, "Already notified today, skipping duplicate")
                //     return@launch
                // }

                // 3. 알림 표시
                val shown = notificationHelper.showRainNotification(pop, precipType)
                Log.d(TAG, "Rain notification shown=$shown, pop=$pop, precipType=$precipType")

                // 3-1. 풀스크린 Alert 화면 표시
                launchAlertActivity(context, pop, precipType)

                // 4. 알림 실제로 표시된 경우에만 기록
                if (shown) {
                    preferencesRepository.markNotificationShown()
                }

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

    private fun launchAlertActivity(context: Context, pop: Int, precipType: PrecipitationType) {
        try {
            val alertIntent = Intent(context, AlarmAlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(AlarmAlertActivity.EXTRA_POP, pop)
                putExtra(AlarmAlertActivity.EXTRA_PRECIP_TYPE, precipType.name)
            }
            context.startActivity(alertIntent)
            Log.d(TAG, "AlarmAlertActivity launched: pop=$pop, precipType=$precipType")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch AlarmAlertActivity", e)
        }
    }
}
