package com.umbrella.presentation.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.umbrella.domain.model.ManualLocation
import com.umbrella.domain.model.UserSettings
import com.umbrella.util.BatteryOptimizationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.settingsState.collectAsState()
    val showTimePicker by viewModel.showTimePicker.collectAsState()
    val showCityPicker by viewModel.showCityPicker.collectAsState()
    val showBatteryDialog by viewModel.showBatteryDialog.collectAsState()
    val showDiagnosticDialog by viewModel.showDiagnosticDialog.collectAsState()
    val diagnosticInfo by viewModel.diagnosticInfo.collectAsState()

    // 화면 복귀 시 배터리 최적화 상태 새로고침
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshBatteryOptimizationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 알림 활성화
            SettingsSwitch(
                title = "알림 활성화",
                subtitle = if (uiState.settings.isEnabled) "켜짐" else "꺼짐",
                icon = Icons.Default.Check,
                checked = uiState.settings.isEnabled,
                onCheckedChange = { viewModel.updateEnabled(it) }
            )

            HorizontalDivider()

            // 알림 시간
            SettingsItem(
                title = "알림 시간",
                subtitle = formatTime(uiState.settings.notificationTime),
                icon = Icons.Default.AccessTime,
                onClick = { viewModel.showTimePicker() }
            )

            HorizontalDivider()

            // 강수확률 임계치
            ThresholdSlider(
                threshold = uiState.settings.popThreshold,
                onThresholdChange = { viewModel.updatePopThreshold(it) }
            )

            HorizontalDivider()

            // 수동 위치 설정
            SettingsItem(
                title = "수동 위치 설정",
                subtitle = uiState.settings.manualLocation?.cityName ?: "설정 안 됨 (GPS 사용)",
                icon = Icons.Default.LocationOn,
                onClick = { viewModel.showCityPicker() }
            )

            HorizontalDivider()

            // 배터리 최적화 설정
            SettingsItem(
                title = "배터리 최적화",
                subtitle = if (uiState.isBatteryOptimizationIgnored) "제외됨 (권장)" else "최적화 중 (알림 지연 가능)",
                icon = Icons.Default.BatteryAlert,
                onClick = { viewModel.showBatteryDialog() }
            )

            HorizontalDivider()

            // 진단 정보
            SettingsItem(
                title = "진단 정보",
                subtitle = "앱 상태 및 설정값 확인",
                icon = Icons.Default.Info,
                onClick = { viewModel.showDiagnosticDialog() }
            )

            // 경고 배너들
            Spacer(modifier = Modifier.height(16.dp))

            if (!uiState.canScheduleExact) {
                ClickableWarningBanner(
                    message = "정확한 알람 권한이 없어 알림이 최대 15분 지연될 수 있습니다. 탭하여 설정",
                    onClick = {
                        try {
                            context.startActivity(viewModel.getExactAlarmSettingsIntent())
                        } catch (e: Exception) {
                            // 일부 기기에서 직접 이동 불가
                            context.startActivity(viewModel.getAppDetailsIntent())
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!uiState.isBatteryOptimizationIgnored) {
                WarningBanner(
                    message = "배터리 최적화로 인해 알림이 지연될 수 있습니다. 설정에서 제외해주세요."
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 시간 선택 다이얼로그
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = uiState.settings.notificationTime.hour,
            initialMinute = uiState.settings.notificationTime.minute,
            onConfirm = { hour, minute ->
                viewModel.updateNotificationTime(hour, minute)
                viewModel.hideTimePicker()
            },
            onDismiss = { viewModel.hideTimePicker() }
        )
    }

    // 도시 선택 다이얼로그
    if (showCityPicker) {
        CityPickerDialog(
            cities = uiState.cities,
            selectedCity = uiState.settings.manualLocation,
            onCitySelected = { city ->
                viewModel.updateManualLocation(city)
                viewModel.hideCityPicker()
            },
            onDismiss = { viewModel.hideCityPicker() }
        )
    }

    // 배터리 최적화 안내 다이얼로그
    if (showBatteryDialog) {
        BatteryOptimizationDialog(
            isIgnored = uiState.isBatteryOptimizationIgnored,
            onOpenSettings = {
                try {
                    context.startActivity(viewModel.getBatteryOptimizationIntent())
                } catch (e: Exception) {
                    // 일부 기기에서 직접 이동 불가 시 앱 설정으로
                    context.startActivity(viewModel.getAppDetailsIntent())
                }
                viewModel.hideBatteryDialog()
            },
            onDismiss = { viewModel.hideBatteryDialog() }
        )
    }

    // 진단 정보 다이얼로그
    if (showDiagnosticDialog) {
        DiagnosticDialog(
            info = diagnosticInfo,
            onDismiss = { viewModel.hideDiagnosticDialog() }
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
fun ThresholdSlider(
    threshold: Int,
    onThresholdChange: (Int) -> Unit
) {
    var sliderValue by remember(threshold) { mutableFloatStateOf(threshold.toFloat()) }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Water,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("강수확률 임계치")
            }
            Text(
                "${sliderValue.toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onThresholdChange(sliderValue.toInt()) },
            valueRange = UserSettings.MIN_THRESHOLD.toFloat()..UserSettings.MAX_THRESHOLD.toFloat(),
            steps = (UserSettings.MAX_THRESHOLD - UserSettings.MIN_THRESHOLD) / UserSettings.THRESHOLD_STEP - 1
        )

        Text(
            "이 확률 이상이면 알림을 보냅니다",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알림 시간 설정") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                TimePicker(state = timePickerState)
                Text(
                    "오전 5시 ~ 9시 사이로 설정하세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    // 시간 범위 검증 (05:00 ~ 09:00)
                    if (hour in 5..9) {
                        onConfirm(hour, minute)
                    } else {
                        onConfirm(7, 30) // 범위 밖이면 기본값
                    }
                }
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
fun CityPickerDialog(
    cities: List<ManualLocation>,
    selectedCity: ManualLocation?,
    onCitySelected: (ManualLocation?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("지역 선택") },
        text = {
            LazyColumn {
                // GPS 사용 옵션
                item {
                    ListItem(
                        headlineContent = { Text("GPS 사용 (자동)") },
                        trailingContent = {
                            if (selectedCity == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "선택됨",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable { onCitySelected(null) }
                    )
                    HorizontalDivider()
                }

                items(cities) { city ->
                    ListItem(
                        headlineContent = { Text(city.cityName) },
                        trailingContent = {
                            if (selectedCity?.cityName == city.cityName) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "선택됨",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable { onCitySelected(city) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

@Composable
fun BatteryOptimizationDialog(
    isIgnored: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(BatteryOptimizationHelper.GUIDANCE_TITLE) },
        text = {
            Column {
                if (isIgnored) {
                    Text(
                        "현재 배터리 최적화에서 제외되어 있습니다. 알림이 정상적으로 작동합니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        BatteryOptimizationHelper.GUIDANCE_MESSAGE.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            if (!isIgnored) {
                TextButton(onClick = onOpenSettings) {
                    Text("설정 열기")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isIgnored) "확인" else "나중에")
            }
        }
    )
}

@Composable
fun WarningBanner(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ClickableWarningBanner(message: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun DiagnosticDialog(
    info: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("진단 정보") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    info,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}

private fun formatTime(time: kotlinx.datetime.LocalTime): String {
    val hour = time.hour
    val minute = time.minute
    val amPm = if (hour < 12) "오전" else "오후"
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return "$amPm ${displayHour}:${minute.toString().padStart(2, '0')}"
}
