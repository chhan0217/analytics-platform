# 화면 체류시간 설계

> 작성일: 2026-10-26

## 1. 목적

사용자가 각 화면에 얼마나 머물렀는지 정확하게 측정한다.
SDK에서 화면을 떠날 때 `screen_leave` 이벤트를 전송하여 체류시간을 기록한다.

## 2. SDK 구현

### 이벤트 설계

화면 진입 시 `screen_view`는 기존과 동일. 화면을 떠날 때 `screen_leave`를 추가 전송:

```json
{
  "event_key": "screen_leave",
  "properties": {
    "screen_id": "main/home",
    "duration_ms": 150000
  }
}
```

### TrackScreenEvents 동작 변경

```
화면 A 진입 → screen_view (screen_id: A) + 타이머 시작
화면 B 진입 → screen_leave (screen_id: A, duration_ms: 차이) + screen_view (screen_id: B) + 타이머 시작
```

- `duration_ms`: 이전 화면 진입 시각과 현재 시각의 차이 (밀리초)
- 첫 화면은 이전 화면이 없으므로 `screen_leave` 미전송
- 앱 백그라운드 진입 시 마지막 화면의 `screen_leave` 전송 (`onBackground`)

## 3. 텍소노미

서버 텍소노미에 등록:

| 이벤트 | 속성 | 타입 | 필수 |
|--------|------|------|------|
| `screen_leave` | `screen_id` | STRING | O |
| `screen_leave` | `duration_ms` | INTEGER | O |

## 4. 서버 API

기존 화면 분석 API(`/analytics/screens`)에 평균 체류시간 추가:

### 응답

```json
{
  "data": {
    "screens": [
      {
        "screen_id": "main/home",
        "view_count": 50,
        "unique_users": 10,
        "avg_duration_ms": 45000
      }
    ]
  }
}
```

- `avg_duration_ms`: 해당 화면의 `screen_leave` 이벤트들의 `duration_ms` 평균

## 5. 대시보드

기존 화면 분석 페이지 테이블에 **평균 체류시간** 컬럼 추가:

```
화면               조회수   사용자   평균 체류시간
main/home            50      10      45초
main/membership      30       8      1분 20초
login                15       5      30초
```

## 6. 구현 범위

| 레이어 | 파일 | 변경 |
|--------|------|------|
| SDK (Compose) | `AnalyticsNavigation.kt` | `screen_leave` + `duration_ms` 전송 로직 |
| SDK (KMP) | `Analytics.kt` | `trackScreenLeave()` 메서드 추가 |
| Backend | `AnalyticsService.kt` | `screens()` 에 평균 체류시간 집계 추가 |
| Backend | `AnalyticsDto.kt` | `ScreenEntry`에 `avgDurationMs` 필드 추가 |
| Backend | 텍소노미 | `screen_leave` 이벤트 + 속성 등록 |
| Dashboard | 화면 분석 페이지 | 평균 체류시간 컬럼 표시 |

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-10-26 | 초안 작성 |
