package com.umbrella.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 배터리 최적화 예외 설정 헬퍼
 */
@Singleton
class BatteryOptimizationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 배터리 최적화에서 제외되어 있는지 확인
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true // Android 6.0 미만은 해당 없음
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * 배터리 최적화 설정 화면으로 이동하는 Intent
     */
    fun getBatteryOptimizationSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    /**
     * 앱 세부 설정 화면으로 이동하는 Intent
     * (일부 기기에서 배터리 최적화 직접 이동이 안 될 때 대안)
     */
    fun getAppDetailsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * 정확한 알람 권한 설정 화면으로 이동하는 Intent (Android 12+)
     */
    fun getExactAlarmSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            getAppDetailsSettingsIntent()
        }
    }

    companion object {
        /**
         * 안내 문구 (과장 없이 제한 설명)
         */
        const val GUIDANCE_MESSAGE = """
일부 기기에서 배터리 절약 기능으로 인해 알림이 지연되거나 누락될 수 있습니다.

정상 동작을 위해 다음을 권장합니다:
• 배터리 최적화에서 Umbrella 앱 제외
• (삼성) 앱 설정 → 배터리 → 백그라운드 활동 허용

이 설정을 하지 않아도 앱은 작동하지만,
알림 시간이 지연될 수 있습니다.
"""

        const val GUIDANCE_TITLE = "배터리 최적화 안내"
    }
}
