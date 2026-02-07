package com.umbrella.data.scheduler

import kotlinx.datetime.LocalTime

/**
 * 알림 예약 인터페이스
 */
interface NotificationScheduler {

    /**
     * 알림 예약
     * @param time 알림 시간 (목표 시간)
     * @param pop 강수확률
     * @return 예약 결과 (성공/실패 포함)
     */
    suspend fun scheduleNotification(time: LocalTime, pop: Int): AlarmScheduleResult

    /**
     * 예약된 알림 취소
     */
    suspend fun cancelScheduledNotification()

    /**
     * 현재 예약 상태 확인
     */
    suspend fun getScheduledInfo(): ScheduleInfo?

    /**
     * 정확한 알람 사용 가능 여부 (시스템 권한 체크)
     */
    fun canScheduleExactAlarms(): Boolean
}

/**
 * 알람 스케줄링 결과 - sealed class로 성공/실패 명확히 구분
 *
 * 주의: domain.model.ScheduleResult와 다름 (UseCase 레이어용)
 */
sealed class AlarmScheduleResult {
    /**
     * 스케줄링 성공
     * @param info 예약된 알람 정보
     */
    data class Success(val info: ScheduleInfo) : AlarmScheduleResult()

    /**
     * 스케줄링 실패
     * @param reason 실패 이유
     * @param exception 원인 예외 (있는 경우)
     */
    data class Failure(
        val reason: FailureReason,
        val exception: Throwable? = null
    ) : AlarmScheduleResult()
}

/**
 * 스케줄링 실패 이유
 */
enum class FailureReason {
    /** 정확한 알람 권한 없음 (Android 12+ SCHEDULE_EXACT_ALARM) */
    EXACT_ALARM_PERMISSION_DENIED,

    /** 알람 설정 중 보안 예외 (권한 철회 등) */
    SECURITY_EXCEPTION,

    /** 잘못된 시간 (과거 시간 등) */
    INVALID_TIME,

    /** 알 수 없는 오류 */
    UNKNOWN_ERROR;

    /**
     * 사용자에게 표시할 메시지
     */
    fun toUserMessage(): String = when (this) {
        EXACT_ALARM_PERMISSION_DENIED ->
            "정확한 알람 권한이 필요합니다. 설정에서 권한을 허용해주세요."
        SECURITY_EXCEPTION ->
            "알람 설정 권한이 거부되었습니다. 앱 설정을 확인해주세요."
        INVALID_TIME ->
            "잘못된 알림 시간입니다."
        UNKNOWN_ERROR ->
            "알람 설정 중 오류가 발생했습니다."
    }
}

/**
 * 예약 정보 - 실제 스케줄 결과 기반
 *
 * @param targetTimeMillis 목표 알림 시간 (사용자가 원하는 시간, UI 표시용)
 * @param triggerTimeMillis 실제 트리거 시간 (AlarmManager에 등록된 시간)
 * @param isExact 정확한 알람 여부 (실제로 exact로 등록되었는지)
 * @param bufferApplied 버퍼 적용 여부 (inexact일 때 10분 앞당김)
 * @param bufferMinutes 적용된 버퍼 시간 (분)
 * @param pop 강수확률
 */
data class ScheduleInfo(
    val targetTimeMillis: Long,
    val triggerTimeMillis: Long,
    val isExact: Boolean,
    val bufferApplied: Boolean,
    val bufferMinutes: Int,
    val pop: Int
) {
    /**
     * 버퍼 적용으로 인한 시간 차이 (밀리초)
     */
    val bufferDeltaMillis: Long
        get() = targetTimeMillis - triggerTimeMillis

    /**
     * 진단용 요약 문자열
     */
    fun toDiagnosticString(): String = buildString {
        appendLine("목표 시간: ${formatMillisToDateTime(targetTimeMillis)}")
        appendLine("트리거 시간: ${formatMillisToDateTime(triggerTimeMillis)}")
        appendLine("알람 유형: ${if (isExact) "정확 (Exact)" else "비정확 (Inexact)"}")
        if (bufferApplied) {
            appendLine("버퍼: ${bufferMinutes}분 앞당김")
        }
        appendLine("강수확률: ${pop}%")
    }

    companion object {
        private fun formatMillisToDateTime(millis: Long): String {
            val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
            val localDateTime = instant.toLocalDateTime(kotlinx.datetime.TimeZone.of("Asia/Seoul"))
            return "${localDateTime.date} ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
        }
    }
}
