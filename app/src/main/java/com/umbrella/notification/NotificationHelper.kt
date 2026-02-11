package com.umbrella.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.umbrella.R
import com.umbrella.domain.model.PrecipitationType
import com.umbrella.presentation.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "umbrella_rain_alert"
        const val CHANNEL_NAME = "비/눈 알림"
        const val CHANNEL_DESCRIPTION = "내일 비 또는 눈 예보가 있을 때 알림을 보냅니다"

        private const val NOTIFICATION_ID = 1
    }

    init {
        createNotificationChannel()
    }

    /**
     * 알림 채널 생성 (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 강수 알림 표시 (비/눈/혼합)
     */
    fun showRainNotification(
        pop: Int,
        precipitationType: PrecipitationType = PrecipitationType.RAIN
    ): Boolean {
        if (!hasNotificationPermission()) {
            return false
        }

        val (title, text) = when (precipitationType) {
            PrecipitationType.RAIN -> "☔ 우산 챙기세요!" to "오늘 비 올 확률 ${pop}%"
            PrecipitationType.SNOW -> "❄️ 눈이 와요!" to "오늘 눈 올 확률 ${pop}%"
            PrecipitationType.MIXED -> "🌨️ 비/눈 소식!" to "오늘 비 또는 눈 올 확률 ${pop}%"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_umbrella)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    /**
     * 알림 권한 확인
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 알림 채널 활성화 여부 확인
     */
    fun isChannelEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = manager.getNotificationChannel(CHANNEL_ID)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        return true
    }
}
