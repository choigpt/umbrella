package com.umbrella.domain.usecase

import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.domain.model.AppStatus
import com.umbrella.domain.model.ScheduleResult
import javax.inject.Inject

/**
 * 수동 새로고침 UseCase
 *
 * 날씨 조회 → 알림 예약까지 전체 플로우 실행
 */
class RefreshWeatherUseCase @Inject constructor(
    private val checkWeatherUseCase: CheckWeatherUseCase,
    private val scheduleNotificationUseCase: ScheduleNotificationUseCase,
    private val preferencesRepository: PreferencesRepository
) {

    suspend operator fun invoke(): RefreshResult {
        // 진행 중 상태 업데이트
        preferencesRepository.updateStatus(AppStatus.CHECKING)

        // 날씨 조회
        val decision = checkWeatherUseCase(forceRefresh = true)

        // 알림 예약
        val scheduleResult = scheduleNotificationUseCase(decision)

        return when (scheduleResult) {
            is ScheduleResult.Scheduled -> RefreshResult.Success(
                message = "비 예보 확인됨 (${scheduleResult.pop}%)",
                isScheduled = true
            )
            is ScheduleResult.Cancelled -> RefreshResult.Success(
                message = "비 예보 없음",
                isScheduled = false
            )
            is ScheduleResult.Failed -> RefreshResult.Failed(
                message = scheduleResult.reason
            )
        }
    }
}

sealed class RefreshResult {
    data class Success(
        val message: String,
        val isScheduled: Boolean
    ) : RefreshResult()

    data class Failed(
        val message: String
    ) : RefreshResult()
}
