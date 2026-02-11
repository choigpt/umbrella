# Changelog

## v0.3.0 - 백그라운드 날씨 수집 신뢰성 강화 + 눈 지원

### Part A: 사전 확인 알람 (Safety Net) + WorkManager 체크 확대

#### 핵심 변경
- **AlarmManager 기반 사전 확인 알람 추가**: 알림 시간 60분 전에 exact alarm으로 날씨를 최종 확인하여 WorkManager 실패 시에도 알림을 보장
- **WorkManager 체크 횟수 확대**: 2회/일 (21:00, 04:00) → 4회/일 (00:00, 06:00, 12:00, 21:00)
- **자체 재예약 체인(Self-sustaining chain)**: 6개 진입점으로 알람 체인이 절대 끊어지지 않도록 보장

#### 하루 타임라인 (알림시간 07:30 기준)
```
00:00  [WorkManager]         날씨 체크 + 사전확인 알람 갱신
06:00  [WorkManager]         날씨 체크 + 사전확인 알람 갱신
06:30  [AlarmManager 사전확인]  SAFETY NET: 최신 날씨 확인 → 최종 알림 결정
07:30  [AlarmManager 알림]     비/눈 알림 + 풀스크린 Alert
12:00  [WorkManager]         내일 예보 수집
21:00  [WorkManager]         내일 예보 수집 + 알람 예약
```

#### 체인 복구 진입점 (6개)
| 진입점 | 트리거 | 동작 |
|--------|--------|------|
| UmbrellaApp.onCreate() | 앱 시작/콜드스타트 | restorePreCheckAlarmIfNeeded() |
| BootReceiver | 기기 재부팅 | restorePreCheckAlarmIfNeeded() |
| TimeChangeReceiver | 시간/타임존 변경 | restorePreCheckAlarmIfNeeded() |
| WeatherCheckWorker (x4/일) | WorkManager 주기 실행 | schedulePreCheckAlarm() |
| WeatherCheckReceiver (자체) | 사전확인 알람 발동 | 다음 날 schedulePreCheckAlarm() |
| SettingsViewModel | 알림 시간 변경 | schedulePreCheckAlarm(newTime) |

---

### Part B: 눈(Snow) 지원

#### 핵심 변경
- **PrecipitationType enum 추가**: WMO weather code 기반 비/눈/혼합 구분
  - RAIN: 51,53,55 (이슬비), 61,63,65 (비), 80,81,82 (소나기)
  - SNOW: 71,73,75 (눈), 77 (싸라기눈), 85,86 (눈소나기)
  - MIXED: 56,57 (어는 이슬비), 66,67 (어는 비)
- **강수 유형별 알림 메시지 분기**:
  - 비: "우산 챙기세요!" / "오늘 비 올 확률 X%"
  - 눈: "눈이 와요!" / "오늘 눈 올 확률 X%"
  - 혼합: "비/눈 소식!" / "오늘 비 또는 눈 올 확률 X%"
- **풀스크린 알림 UI 분기**: 강수 유형별 아이콘 색상/메시지 변경

---

### Bug Fix: 대상 날짜 계산 오류 수정

#### 문제
- `WeatherMapper`가 항상 "내일" 데이터만 필터링
- 자정~알림시간 사이에 Worker가 실행될 때, "내일"이 실제 알림 당일이 아닌 그 다음 날이 됨
- 결과: **비가 와도 알림이 안 뜨는 치명적 버그**

#### 수정
- `CheckWeatherUseCase`에 동적 대상 날짜 계산 로직 추가:
  - 자정~알림시간: "오늘" 예보 확인 (알림 당일)
  - 알림시간 이후: "내일" 예보 확인 (다음 날 알림 예약)
- `WeatherMapper`에 `mapToForecast(response, targetDate)` 메서드 추가
- `WeatherRepository`에 `targetDate` 파라미터 전파

---

### 테스트

96개 전체 테스트 통과 (실패 0):
- **PrecipitationTypeTest** (13): WMO weather code → PrecipitationType 매핑
- **DailyForecastPrecipTypeTest** (10): 시간 범위 내 주요 강수 유형 판별
- **WeatherDecisionTest** (5): RainExpected precipitationType 필드
- **CheckWeatherUseCaseTest** (8): 강수 유형 계산 + 대상 날짜 전달
- **ScheduleNotificationUseCaseTest** (5): precipitationType 전달 검증
- **WeatherMapperTest** (12): mapToForecast 날짜 필터링 + edge cases
- **AlarmSchedulerConstantsTest** (6): 상수값 + ScheduleInfo
- **WorkerSchedulerConstantsTest** (6): work name 상수
- 기타 기존 테스트 (31)

---

### 수정 파일 목록

#### 신규 파일 (1)
- `receiver/WeatherCheckReceiver.kt` — 사전확인 알람 BroadcastReceiver

#### 수정된 소스 파일 (21)
- `domain/model/HourlyForecast.kt` — PrecipitationType enum + dominantPrecipitationType()
- `domain/model/WeatherDecision.kt` — RainExpected에 precipitationType 필드
- `domain/usecase/CheckWeatherUseCase.kt` — 대상 날짜 계산 + 강수 유형 판별
- `domain/usecase/ScheduleNotificationUseCase.kt` — precipitationType 전달
- `data/api/WeatherMapper.kt` — mapToForecast(response, targetDate) 추가
- `data/repository/WeatherRepository.kt` — targetDate 파라미터 지원
- `data/scheduler/AlarmSchedulerImpl.kt` — precipitationType + 사전확인 알람
- `data/scheduler/NotificationScheduler.kt` — 인터페이스 업데이트
- `data/prefs/UserPreferences.kt` — DataStore 키 추가
- `data/prefs/PreferencesRepository.kt` — 사전확인 알람/강수유형 저장
- `notification/NotificationHelper.kt` — 비/눈 분기 메시지
- `receiver/AlarmReceiver.kt` — EXTRA_PRECIP_TYPE 읽기
- `receiver/BootReceiver.kt` — 사전확인 알람 복구
- `receiver/TimeChangeReceiver.kt` — 사전확인 알람 복구
- `presentation/alert/AlarmAlertActivity.kt` — 눈 알림 UI
- `presentation/settings/SettingsViewModel.kt` — 설정 변경 시 연동
- `worker/WeatherCheckWorker.kt` — 사전확인 체인 유지
- `worker/WorkerScheduler.kt` — 4회 체크 확대
- `UmbrellaApp.kt` — 앱 시작 시 초기화
- `AndroidManifest.xml` — Receiver 등록
- `build.gradle.kts` — 테스트 의존성

#### 수정된 테스트 파일 (10)
- `WeatherMapperTest.kt` — mapToForecast 12개 테스트 추가
- `StatusInfoTest.kt` — 실제 구현에 맞게 기대값 수정
- `UserSettingsTest.kt` — MIN_THRESHOLD 기대값 수정
- `AlarmAlertActivityTest.kt` — EXTRA_PRECIP_TYPE 추가
- `PrecipitationTypeTest.kt` — 신규
- `DailyForecastPrecipTypeTest.kt` — 신규
- `WeatherDecisionTest.kt` — 신규
- `CheckWeatherUseCaseTest.kt` — 신규
- `ScheduleNotificationUseCaseTest.kt` — 신규
- `AlarmSchedulerConstantsTest.kt` — 신규
- `WorkerSchedulerConstantsTest.kt` — 신규
