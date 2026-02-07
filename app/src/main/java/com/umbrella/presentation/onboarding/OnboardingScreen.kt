package com.umbrella.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (currentStep) {
            0 -> WelcomeStep(
                onNext = { currentStep = 1 }
            )
            1 -> NotificationPermissionStep(
                onRequestPermission = onRequestNotificationPermission,
                onNext = { currentStep = 2 }
            )
            2 -> LocationPermissionStep(
                onRequestPermission = onRequestLocationPermission,
                onNext = { currentStep = 3 }
            )
            3 -> CompletionStep(
                onComplete = onComplete
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 진행 표시
        StepIndicator(
            currentStep = currentStep,
            totalSteps = 4
        )
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    OnboardingCard(
        icon = null,
        title = "☔ Umbrella",
        description = "비 오는 날 아침,\n우산 챙기라고 알려드릴게요.\n\n간단한 설정만 하면 됩니다.",
        buttonText = "시작하기",
        onButtonClick = onNext
    )
}

@Composable
fun NotificationPermissionStep(
    onRequestPermission: () -> Unit,
    onNext: () -> Unit
) {
    OnboardingCard(
        icon = Icons.Default.Notifications,
        title = "알림 권한",
        description = "비 예보가 있을 때 아침에 알림을 보내드려요.\n알림 권한을 허용해주세요.",
        buttonText = "권한 허용",
        onButtonClick = {
            onRequestPermission()
            onNext()
        },
        secondaryButtonText = "나중에",
        onSecondaryClick = onNext
    )
}

@Composable
fun LocationPermissionStep(
    onRequestPermission: () -> Unit,
    onNext: () -> Unit
) {
    OnboardingCard(
        icon = Icons.Default.LocationOn,
        title = "위치 권한",
        description = "현재 위치의 날씨를 확인하기 위해\n위치 권한이 필요해요.\n\n(또는 설정에서 도시를 직접 선택할 수 있어요)",
        buttonText = "권한 허용",
        onButtonClick = {
            onRequestPermission()
            onNext()
        },
        secondaryButtonText = "수동으로 설정할게요",
        onSecondaryClick = onNext
    )
}

@Composable
fun CompletionStep(onComplete: () -> Unit) {
    OnboardingCard(
        icon = Icons.Default.CheckCircle,
        title = "준비 완료!",
        description = "설정이 완료되었어요.\n\n매일 저녁 9시에 내일 날씨를 확인하고,\n비 예보가 있으면 아침에 알려드릴게요.",
        buttonText = "시작하기",
        onButtonClick = onComplete
    )
}

@Composable
fun OnboardingCard(
    icon: ImageVector?,
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Text(
                    text = "☔",
                    fontSize = 64.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }

            if (secondaryButtonText != null && onSecondaryClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSecondaryClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(secondaryButtonText)
                }
            }
        }
    }
}

@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int
) {
    Text(
        text = "${currentStep + 1} / $totalSteps",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
