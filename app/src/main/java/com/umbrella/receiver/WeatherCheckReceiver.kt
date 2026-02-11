package com.umbrella.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.data.scheduler.AlarmSchedulerImpl
import com.umbrella.domain.usecase.CheckWeatherUseCase
import com.umbrella.domain.usecase.ScheduleNotificationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 사전확인 알람 수신 BroadcastReceiver
 *
 * 알림 시간 60분 전에 AlarmManager에 의해 트리거됨.
 * WorkManager와 독립적으로 동작하여 날씨 체크 + 알림 예약의 Safety Net 역할.
 *
 * 처리 흐름:
 * 1. 앱 활성화 확인
 * 2. CheckWeatherUseCase(forceRefresh=true) 실행
 * 3. ScheduleNotificationUseCase(decision) 실행
 * 4. 다음 날 사전확인 알람 재예약 (자체 유지 체인)
 */
@AndroidEntryPoint
class WeatherCheckReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WeatherCheckReceiver"
    }

    @Inject
    lateinit var checkWeatherUseCase: CheckWeatherUseCase

    @Inject
    lateinit var scheduleNotificationUseCase: ScheduleNotificationUseCase

    @Inject
    lateinit var alarmScheduler: AlarmSchedulerImpl

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmSchedulerImpl.ACTION_WEATHER_PRE_CHECK) {
            return
        }

        Log.d(TAG, "Pre-check alarm received")
        val pendingResult = goAsync()

        scope.launch {
            try {
                // 1. 앱 활성화 확인
                if (!preferencesRepository.isAppEnabled()) {
                    Log.d(TAG, "App disabled, skipping pre-check")
                    return@launch
                }

                // 2. 날씨 조회
                val decision = checkWeatherUseCase(forceRefresh = true)
                Log.d(TAG, "Pre-check weather decision: $decision")

                // 3. 알림 예약
                val result = scheduleNotificationUseCase(decision)
                Log.d(TAG, "Pre-check schedule result: $result")

            } catch (e: Exception) {
                Log.e(TAG, "Error in pre-check", e)
            } finally {
                // 4. 다음 날 사전확인 알람 재예약 (에러가 나도 반드시 실행)
                try {
                    val settings = preferencesRepository.settingsFlow.first()
                    if (settings.isEnabled) {
                        val scheduled = alarmScheduler.schedulePreCheckAlarm(settings.notificationTime)
                        Log.d(TAG, "Next pre-check alarm scheduled: $scheduled")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule pre-check alarm", e)
                }
            }
        }.invokeOnCompletion {
            pendingResult.finish()
        }
    }
}
