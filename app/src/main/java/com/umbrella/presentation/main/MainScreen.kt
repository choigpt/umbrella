package com.umbrella.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.umbrella.domain.model.AppStatus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onRefresh: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("☔ Umbrella") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 메인 상태 카드
            StatusCard(
                uiState = uiState,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 경고 카드들
            if (uiState.showPermissionWarning) {
                WarningCard(
                    message = "알림 권한이 필요합니다",
                    actionLabel = "권한 허용",
                    onAction = onRequestNotificationPermission,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.showLocationWarning) {
                WarningCard(
                    message = "위치 권한을 허용하거나 수동으로 지역을 설정하세요",
                    actionLabel = "권한 허용",
                    onAction = onRequestLocationPermission,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.showExactAlarmWarning) {
                WarningCard(
                    message = "정확한 시간 알림을 위해 권한이 필요합니다 (선택)",
                    actionLabel = "설정",
                    onAction = onOpenExactAlarmSettings,
                    isOptional = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // 새로고침 버튼
            Button(
                onClick = onRefresh,
                enabled = !uiState.isRefreshing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("확인 중...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("지금 확인하기")
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val status = uiState.statusInfo.status
    val (icon, iconColor) = getStatusIconAndColor(status)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = iconColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = uiState.statusInfo.userMessage,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            uiState.statusInfo.detailMessage?.let { detail ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 위치 정보
            uiState.statusInfo.locationName?.let { location ->
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 마지막 업데이트 시간
            uiState.statusInfo.lastUpdateTime?.let { time ->
                Spacer(modifier = Modifier.height(4.dp))
                val localTime = time.toLocalDateTime(TimeZone.of("Asia/Seoul"))
                Text(
                    text = "마지막 업데이트: ${localTime.hour}:${localTime.minute.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WarningCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    isOptional: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isOptional) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isOptional) Icons.Default.Warning else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isOptional) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun getStatusIconAndColor(status: AppStatus): Pair<ImageVector, Color> {
    return when (status) {
        AppStatus.SCHEDULED_EXACT,
        AppStatus.SCHEDULED_APPROXIMATE -> Icons.Default.CheckCircle to Color(0xFF4CAF50)

        AppStatus.NO_RAIN_EXPECTED -> Icons.Default.WbSunny to Color(0xFFFF9800)

        AppStatus.FETCH_FAILED_NETWORK,
        AppStatus.FETCH_FAILED_LOCATION,
        AppStatus.FETCH_FAILED_API -> Icons.Default.Error to Color(0xFFD32F2F)

        AppStatus.PERMISSION_MISSING_NOTIFICATION,
        AppStatus.PERMISSION_MISSING_LOCATION,
        AppStatus.EXACT_ALARM_UNAVAILABLE -> Icons.Default.Warning to Color(0xFFFF9800)

        else -> Icons.Default.Warning to Color(0xFF757575)
    }
}
