package com.umbrella.domain.usecase

import com.umbrella.data.prefs.PreferencesRepository
import com.umbrella.data.scheduler.AlarmScheduleResult
import com.umbrella.data.scheduler.NotificationScheduler
import com.umbrella.domain.model.AppStatus
import com.umbrella.domain.model.ScheduleResult
import com.umbrella.domain.model.WeatherDecision
import com.umbrella.notification.FailureNotificationHelper
import javax.inject.Inject

/**
 * 알림 예약 UseCase
 *
 * WeatherDecision에 따라:
 * - RainExpected → 알림 예약
 * - NoRain → 예약 취소
 * - Error → 실패 처리 (연속 3회 실패 시 사용자 알림)
 *
 * AlarmScheduleResult (스케줄러) → ScheduleResult (도메인) 변환
 */
class ScheduleNotificationUseCase @Inject constructor(
    private val scheduler: NotificationScheduler,
    private val preferencesRepository: PreferencesRepository,
    private val failureNotificationHelper: FailureNotificationHelper
) {

    suspend operator fun invoke(decision: WeatherDecision): ScheduleResult {
        return when (decision) {
            is WeatherDecision.RainExpected -> {
                handleRainExpected(decision)
            }

            is WeatherDecision.NoRain -> {
                handleNoRain(decision)
            }

            is WeatherDecision.Error -> {
                handleError(decision)
            }
        }
    }

    /**
     * 비 예보 → 알림 예약
     */
    private suspend fun handleRainExpected(decision: WeatherDecision.RainExpected): ScheduleResult {
        val alarmResult = scheduler.scheduleNotification(
            time = decision.notificationTime,
            pop = decision.maxPop
        )

        return when (alarmResult) {
            is AlarmScheduleResult.Success -> {
                val info = alarmResult.info

                // 상태 업데이트 - 실제 스케줄링 결과 기반
                val status = if (info.isExact) {
                    AppStatus.SCHEDULED_EXACT
                } else {
                    AppStatus.SCHEDULED_APPROXIMATE
                }

                preferencesRepository.updateStatus(
                    status = status,
                    pop = decision.maxPop,
                    locationName = decision.location.name
                )

                // 성공 시 실패 카운터 리셋 및 실패 알림 취소
                preferencesRepository.resetFailureCount()
                failureNotificationHelper.cancelFailureNotification()

                ScheduleResult.Scheduled(
                    isExact = info.isExact,
                    scheduledTime = decision.notificationTime,
                    pop = decision.maxPop
                )
            }

            is AlarmScheduleResult.Failure -> {
                // 알람 스케줄링 실패 시 에러 처리
                val failureCount = preferencesRepository.incrementFailureCount()

                preferencesRepository.updateStatus(
                    status = AppStatus.FETCH_FAILED_API // 스케줄링 실패
                )

                val reason = alarmResult.reason.toUserMessage()

                if (failureCount >= FailureNotificationHelper.FAILURE_THRESHOLD) {
                    failureNotificationHelper.showFailureNotification(
                        failureCount = failureCount,
                        reason = reason
                    )
                }

                ScheduleResult.Failed(
                    reason = "$reason (연속 ${failureCount}회 실패)"
                )
            }
        }
    }

    /**
     * 비 없음 → 예약 취소
     */
    private suspend fun handleNoRain(decision: WeatherDecision.NoRain): ScheduleResult {
        scheduler.cancelScheduledNotification()

        preferencesRepository.updateStatus(
            status = AppStatus.NO_RAIN_EXPECTED,
            pop = decision.maxPop,
            locationName = decision.location.name
        )

        // 성공 시 실패 카운터 리셋 및 실패 알림 취소
        preferencesRepository.resetFailureCount()
        failureNotificationHelper.cancelFailureNotification()

        return ScheduleResult.Cancelled
    }

    /**
     * 날씨 조회 오류
     */
    private suspend fun handleError(decision: WeatherDecision.Error): ScheduleResult {
        val failureCount = preferencesRepository.incrementFailureCount()

        val status = when (decision.type) {
            com.umbrella.domain.model.ErrorType.NETWORK -> AppStatus.FETCH_FAILED_NETWORK
            com.umbrella.domain.model.ErrorType.LOCATION -> AppStatus.FETCH_FAILED_LOCATION
            com.umbrella.domain.model.ErrorType.API -> AppStatus.FETCH_FAILED_API
            com.umbrella.domain.model.ErrorType.UNKNOWN -> AppStatus.FETCH_FAILED_API
        }

        preferencesRepository.updateStatus(status = status)

        val reason = decision.message ?: "알 수 없는 오류"

        // 연속 실패 임계치 도달 시 사용자 알림
        if (failureCount >= FailureNotificationHelper.FAILURE_THRESHOLD) {
            failureNotificationHelper.showFailureNotification(
                failureCount = failureCount,
                reason = reason
            )
        }

        return ScheduleResult.Failed(
            reason = "$reason (연속 ${failureCount}회 실패)"
        )
    }
}
