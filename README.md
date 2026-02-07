# ☔ Umbrella

위치 기반 비 예보 알림 앱 - 비 오는 날 아침에 우산 챙기라고 알려드립니다.

## 주요 기능

- **자동 날씨 확인**: 저녁/새벽 시간대에 내일 날씨 예보 확인 (정확한 시간 보장 안 됨)
- **아침 알림**: 비 예보 시 지정된 시간(기본 07:30)에 알림 (정확한 알람 권한 필요)
- **위치 기반**: GPS 또는 수동 설정 도시 기준
- **오프라인 지원**: 캐시된 데이터로 네트워크 오류 대응

## 기술 스택

- **Kotlin** + **Jetpack Compose**
- **Hilt** (DI)
- **WorkManager** (백그라운드 스케줄링)
- **DataStore** (로컬 저장소)
- **Retrofit** + **Kotlinx Serialization** (API)
- **Open-Meteo API** (무료 날씨 API, 키 불필요)

## 아키텍처

```
Presentation (Compose + ViewModel)
        ↓
    Domain (UseCase)
        ↓
    Data (Repository + API + DataStore)
```

## 스케줄링 전략

### 날씨 데이터 수집 (WorkManager)
- **저녁 조회** (21:00 전후): 내일 예보 확인 → 알림 예약
- **새벽 조회** (04:00 전후): 예보 갱신, 필요시 알림 재예약
- ⚠️ **주의**: PeriodicWorkRequest는 정확한 시간을 보장하지 않음. Doze/배터리 최적화 상태에서 수 시간 지연 가능

### 알림 전달 (AlarmManager)

#### A안: 정확한 알람 (권장)
- `AlarmManager.setExactAndAllowWhileIdle()` 사용
- Android 12+ 에서 `SCHEDULE_EXACT_ALARM` 권한 필요
- 정시 알림 보장 (±수초)
- 설정 > "정확한 알람 권한" 탭하여 허용 필요

#### B안: setAndAllowWhileIdle (Fallback)
- 권한 없거나 거부된 경우 자동 적용
- Doze maintenance window에서만 실행 (최대 15분 지연)
- 10분 버퍼로 앞당겨 예약하지만, Deep Doze에서는 25분까지 지연 가능

## 빌드

```bash
# Android SDK 경로 설정
cp local.properties.example local.properties
# local.properties에서 sdk.dir 경로 수정

# 빌드
./gradlew assembleDebug

# 테스트
./gradlew test
```

## 권한

| 권한 | 용도 | 필수 |
|------|------|------|
| `INTERNET` | API 호출 | ✅ |
| `POST_NOTIFICATIONS` | 알림 표시 (Android 13+) | ✅ |
| `ACCESS_COARSE_LOCATION` | 위치 기반 예보 | ⚠️ (수동 설정 가능) |
| `SCHEDULE_EXACT_ALARM` | 정시 알림 (Android 12+) | ⚠️ (선택) |

## 제한사항

- **Doze 모드**: 정확한 알람 권한 없이는 알림 시간이 지연될 수 있음
- **제조사별 배터리 최적화**: 일부 기기에서 백그라운드 작업 제한 가능
- **위치 정확도**: `COARSE_LOCATION`으로 도시 단위 정확도

## 라이선스

MIT License
