package com.umbrella.presentation.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.data.scheduler.NotificationScheduler
import com.umbrella.domain.model.KoreanCities
import com.umbrella.domain.model.ManualLocation
import com.umbrella.domain.model.UserSettings
import com.umbrella.util.BatteryOptimizationHelper
import com.umbrella.worker.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val workerScheduler: WorkerScheduler,
    private val notificationScheduler: NotificationScheduler,
    private val batteryOptimizationHelper: BatteryOptimizationHelper
) : ViewModel() {

    private val _batteryOptimizationIgnored = MutableStateFlow(
        batteryOptimizationHelper.isIgnoringBatteryOptimizations()
    )

    val settingsState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.settingsFlow,
        _batteryOptimizationIgnored
    ) { settings, batteryIgnored ->
        SettingsUiState(
            settings = settings,
            cities = KoreanCities.cities,
            canScheduleExact = notificationScheduler.canScheduleExactAlarms(),
            isBatteryOptimizationIgnored = batteryIgnored
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    private val _showTimePicker = MutableStateFlow(false)
    val showTimePicker: StateFlow<Boolean> = _showTimePicker.asStateFlow()

    private val _showCityPicker = MutableStateFlow(false)
    val showCityPicker: StateFlow<Boolean> = _showCityPicker.asStateFlow()

    private val _showBatteryDialog = MutableStateFlow(false)
    val showBatteryDialog: StateFlow<Boolean> = _showBatteryDialog.asStateFlow()

    private val _showDiagnosticDialog = MutableStateFlow(false)
    val showDiagnosticDialog: StateFlow<Boolean> = _showDiagnosticDialog.asStateFlow()

    private val _diagnosticInfo = MutableStateFlow("")
    val diagnosticInfo: StateFlow<String> = _diagnosticInfo.asStateFlow()

    fun updateNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationTime(LocalTime(hour, minute))
            // Worker 재스케줄링
            workerScheduler.schedulePeriodicWeatherCheck()
        }
    }

    fun updatePopThreshold(threshold: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePopThreshold(threshold)
        }
    }

    fun updateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateEnabled(enabled)
            if (enabled) {
                workerScheduler.schedulePeriodicWeatherCheck()
            } else {
                workerScheduler.cancelAllWork()
                notificationScheduler.cancelScheduledNotification()
            }
        }
    }

    fun updateManualLocation(location: ManualLocation?) {
        viewModelScope.launch {
            preferencesRepository.updateManualLocation(location)
        }
    }

    fun showTimePicker() {
        _showTimePicker.value = true
    }

    fun hideTimePicker() {
        _showTimePicker.value = false
    }

    fun showCityPicker() {
        _showCityPicker.value = true
    }

    fun hideCityPicker() {
        _showCityPicker.value = false
    }

    fun showBatteryDialog() {
        _showBatteryDialog.value = true
    }

    fun hideBatteryDialog() {
        _showBatteryDialog.value = false
    }

    fun showDiagnosticDialog() {
        viewModelScope.launch {
            _diagnosticInfo.value = preferencesRepository.getDiagnosticInfo()
            _showDiagnosticDialog.value = true
        }
    }

    fun hideDiagnosticDialog() {
        _showDiagnosticDialog.value = false
    }

    fun refreshBatteryOptimizationStatus() {
        _batteryOptimizationIgnored.value = batteryOptimizationHelper.isIgnoringBatteryOptimizations()
    }

    fun getBatteryOptimizationIntent(): Intent {
        return batteryOptimizationHelper.getBatteryOptimizationSettingsIntent()
    }

    fun getAppDetailsIntent(): Intent {
        return batteryOptimizationHelper.getAppDetailsSettingsIntent()
    }

    fun getExactAlarmSettingsIntent(): Intent {
        return batteryOptimizationHelper.getExactAlarmSettingsIntent()
    }
}

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val cities: List<ManualLocation> = KoreanCities.cities,
    val canScheduleExact: Boolean = true,
    val isBatteryOptimizationIgnored: Boolean = true
)
