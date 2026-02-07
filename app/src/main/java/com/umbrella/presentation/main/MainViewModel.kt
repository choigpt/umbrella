package com.umbrella.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbrella.data.scheduler.NotificationScheduler
import com.umbrella.domain.model.AppStatus
import com.umbrella.domain.model.StatusInfo
import com.umbrella.domain.model.UserSettings
import com.umbrella.domain.usecase.GetCurrentStatusUseCase
import com.umbrella.domain.usecase.RefreshResult
import com.umbrella.domain.usecase.RefreshWeatherUseCase
import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.notification.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getCurrentStatusUseCase: GetCurrentStatusUseCase,
    private val refreshWeatherUseCase: RefreshWeatherUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val notificationScheduler: NotificationScheduler,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val uiState: StateFlow<MainUiState> = combine(
        getCurrentStatusUseCase(),
        preferencesRepository.settingsFlow,
        _isRefreshing
    ) { status, settings, isRefreshing ->
        MainUiState(
            statusInfo = status,
            settings = settings,
            isRefreshing = isRefreshing,
            canScheduleExact = notificationScheduler.canScheduleExactAlarms(),
            hasNotificationPermission = notificationHelper.hasNotificationPermission()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                when (val result = refreshWeatherUseCase()) {
                    is RefreshResult.Success -> {
                        _toastMessage.value = result.message
                    }
                    is RefreshResult.Failed -> {
                        _toastMessage.value = "실패: ${result.message}"
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun checkPermissions(): PermissionState {
        val hasNotification = notificationHelper.hasNotificationPermission()
        val canExact = notificationScheduler.canScheduleExactAlarms()

        return PermissionState(
            hasNotificationPermission = hasNotification,
            canScheduleExactAlarms = canExact
        )
    }
}

data class MainUiState(
    val statusInfo: StatusInfo = StatusInfo.INITIAL,
    val settings: UserSettings = UserSettings(),
    val isRefreshing: Boolean = false,
    val canScheduleExact: Boolean = true,
    val hasNotificationPermission: Boolean = false
) {
    val showPermissionWarning: Boolean
        get() = !hasNotificationPermission ||
                statusInfo.status == AppStatus.PERMISSION_MISSING_NOTIFICATION

    val showLocationWarning: Boolean
        get() = statusInfo.status == AppStatus.PERMISSION_MISSING_LOCATION ||
                statusInfo.status == AppStatus.FETCH_FAILED_LOCATION

    val showExactAlarmWarning: Boolean
        get() = !canScheduleExact

    val isError: Boolean
        get() = statusInfo.status.isError

    val requiresAction: Boolean
        get() = statusInfo.status.requiresAction
}

data class PermissionState(
    val hasNotificationPermission: Boolean,
    val canScheduleExactAlarms: Boolean
)
