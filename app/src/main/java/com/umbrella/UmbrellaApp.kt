package com.umbrella

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.umbrella.data.scheduler.AlarmSchedulerImpl
import com.umbrella.worker.WorkerScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class UmbrellaApp : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "UmbrellaApp"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workerScheduler: WorkerScheduler

    @Inject
    lateinit var alarmScheduler: AlarmSchedulerImpl

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // 앱 시작 시 Worker 스케줄링
        workerScheduler.schedulePeriodicWeatherCheck()

        // 사전확인 알람 복구
        applicationScope.launch {
            try {
                val restored = alarmScheduler.restorePreCheckAlarmIfNeeded()
                Log.d(TAG, "Pre-check alarm restore on app start: $restored")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore pre-check alarm", e)
            }
        }
    }
}
