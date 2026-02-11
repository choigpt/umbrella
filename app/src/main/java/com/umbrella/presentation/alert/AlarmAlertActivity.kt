package com.umbrella.presentation.alert

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umbrella.R
import com.umbrella.domain.model.PrecipitationType

/**
 * 풀스크린 강수 알림 화면
 *
 * 반투명 어두운 배경 위에 큰 아이콘과 메시지를 표시.
 * 강수 유형(비/눈/혼합)에 따라 다른 아이콘과 메시지.
 * 화면 아무 곳이나 탭하면 닫힘.
 * 잠금 화면 위에서도 표시 가능.
 */
class AlarmAlertActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AlarmAlertActivity"
        const val EXTRA_POP = "extra_pop"
        const val EXTRA_PRECIP_TYPE = "extra_precip_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockScreenFlags()

        val pop = intent.getIntExtra(EXTRA_POP, 0)
        val precipTypeName = intent.getStringExtra(EXTRA_PRECIP_TYPE)
        val precipType = try {
            precipTypeName?.let { PrecipitationType.valueOf(it) } ?: PrecipitationType.RAIN
        } catch (_: IllegalArgumentException) {
            PrecipitationType.RAIN
        }
        Log.d(TAG, "Alert shown: pop=$pop, precipType=$precipType")

        val (title, subtitle, iconTint) = when (precipType) {
            PrecipitationType.RAIN -> Triple("우산 챙기세요!", "강수확률 ${pop}%", Color.White)
            PrecipitationType.SNOW -> Triple("눈이 와요!", "강수확률 ${pop}%", Color(0xFFADD8E6))
            PrecipitationType.MIXED -> Triple("비/눈 소식!", "강수확률 ${pop}%", Color(0xFFB0C4DE))
        }

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Log.d(TAG, "Alert dismissed by tap")
                        finish()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_umbrella),
                        contentDescription = title,
                        modifier = Modifier.size(120.dp),
                        tint = iconTint
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = "탭하여 닫기",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - alert closed")
    }

    @Suppress("DEPRECATION")
    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Log.d(TAG, "Lock screen flags set (SDK=${Build.VERSION.SDK_INT})")
    }
}
