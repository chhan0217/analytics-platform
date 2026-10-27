# 이벤트별 집계 + 속성별 분석 설계

> 작성일: 2026-10-26

## 1. 목적

현재는 이벤트를 개별 건 단위로만 조회할 수 있다.
이벤트별 집계를 추가하여 **"어떤 이벤트가 얼마나 발생했고, 누가 몇 명 했는가"**를 요약해서 보여준다.
속성별 분석을 추가하여 **"button_click 중 어떤 버튼이 가장 많이 눌렸는가"**를 바로 확인할 수 있게 한다.

## 2. API 설계

### 2.1 이벤트별 집계

```
GET /admin/projects/{projectId}/analytics/events/summary
```

#### 쿼리 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `start_date` | ISO-8601 | O | 시작일 |
| `end_date` | ISO-8601 | O | 종료일 |
| `application_id` | UUID | X | 앱 필터 |

#### 응답

```json
{
  "data": {
    "events": [
      {
        "event_key": "screen_view",
        "count": 41,
        "unique_users": 3,
        "unique_sessions": 4
      },
      {
        "event_key": "button_click",
        "count": 5,
        "unique_users": 1,
        "unique_sessions": 2
      },
      {
        "event_key": "login_success",
        "count": 1,
        "unique_users": 1,
        "unique_sessions": 1
      }
    ],
    "period": {
      "start_date": "2026-10-01T00:00:00Z",
      "end_date": "2026-10-26T23:59:59Z"
    }
  }
}
```

- `count`: 이벤트 발생 횟수
- `unique_users`: 해당 이벤트를 발생시킨 고유 사용자 수 (anonymous_id 기준)
- `unique_sessions`: 해당 이벤트가 발생한 고유 세션 수
- 정렬: count 내림차순

### 2.2 속성별 분석 (Property Breakdown)

```
GET /admin/projects/{projectId}/analytics/events/breakdown
```

#### 쿼리 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `start_date` | ISO-8601 | O | 시작일 |
| `end_date` | ISO-8601 | O | 종료일 |
| `event_key` | string | O | 분석할 이벤트 |
| `property_key` | string | O | 분석할 속성 키 |
| `application_id` | UUID | X | 앱 필터 |

#### 응답

```json
{
  "data": {
    "event_key": "button_click",
    "property_key": "button_id",
    "breakdown": [
      {
        "value": "onboard_social_kakao",
        "count": 2,
        "unique_users": 1
      },
      {
        "value": "onboard_email_login",
        "count": 1,
        "unique_users": 1
      },
      {
        "value": "login_submit",
        "count": 1,
        "unique_users": 1
      },
      {
        "value": "onboard_guest_browse",
        "count": 1,
        "unique_users": 1
      }
    ],
    "period": {
      "start_date": "2026-10-01T00:00:00Z",
      "end_date": "2026-10-26T23:59:59Z"
    }
  }
}
```

- `value`: 속성 값
- `count`: 해당 값의 발생 횟수
- `unique_users`: 해당 값을 발생시킨 고유 사용자 수
- 정렬: count 내림차순

## 3. 대시보드 UI

### 3.1 이벤트 집계 페이지

기존 이벤트 페이지에 **집계 탭** 추가.

```
┌─────────────────────────────────────────────────┐
│ 이벤트 분석          [2026-10-01 ~ 2026-10-26]  │
├─────────────────────────────────────────────────┤
│ [이벤트 목록]  [이벤트 집계]                       │
├─────────────────────────────────────────────────┤
│                                                 │
│ 이벤트              발생 횟수   사용자   세션     │
│ ─────────────────────────────────────────────── │
│ screen_view           41        3       4       │
│ button_click           5        1       2       │
│ session_start          4        1       4       │
│ session_end            3        1       3       │
│ app_open               2        1       2       │
│ login_success          1        1       1       │
│ identify               1        1       1       │
│                                                 │
└─────────────────────────────────────────────────┘
```

### 3.2 속성별 분석 (행 클릭 시)

이벤트 행을 클릭하면 속성별 분석이 펼쳐진다.

```
┌─────────────────────────────────────────────────┐
│ button_click (5건)                              │
│                                                 │
│ 속성 선택: [button_id ▼]                         │
│                                                 │
│ 값                          횟수    사용자       │
│ ──────────────────────────────────────────────── │
│ onboard_social_kakao          2       1          │
│ onboard_email_login           1       1          │
│ login_submit                  1       1          │
│ onboard_guest_browse          1       1          │
│                                                 │
└─────────────────────────────────────────────────┘
```

속성 선택 드롭다운에는 해당 이벤트의 텍소노미에 등록된 속성 키 목록을 표시한다.

## 4. 구현 범위

### Backend

| 파일 | 변경 |
|------|------|
| `AnalyticsDto.kt` | `EventSummary`, `EventBreakdown` 응답 DTO 추가 |
| `AnalyticsService.kt` | `eventSummary()`, `eventBreakdown()` 메서드 추가 |
| `AnalyticsRoute.kt` | `/events/summary`, `/events/breakdown` 엔드포인트 추가 |

### Web Dashboard

| 파일 | 변경 |
|------|------|
| `api/` | 집계 API 클라이언트 추가 |
| `features/events/` | 집계 탭 컴포넌트 추가, 속성별 분석 펼침 UI |

## 5. 쿼리 설계

### 이벤트별 집계 쿼리

```sql
SELECT
  event_key,
  COUNT(*) AS count,
  COUNT(DISTINCT anonymous_id) AS unique_users,
  COUNT(DISTINCT session_id) AS unique_sessions
FROM analytics_events
WHERE project_id = :projectId
  AND occurred_at BETWEEN :startDate AND :endDate
GROUP BY event_key
ORDER BY count DESC
```

### 속성별 분석 쿼리

```sql
SELECT
  properties ->> :propertyKey AS value,
  COUNT(*) AS count,
  COUNT(DISTINCT anonymous_id) AS unique_users
FROM analytics_events
WHERE project_id = :projectId
  AND event_key = :eventKey
  AND occurred_at BETWEEN :startDate AND :endDate
  AND properties ->> :propertyKey IS NOT NULL
GROUP BY value
ORDER BY count DESC
```

> 현재 properties는 TEXT로 저장되어 있어 `->>` 연산자 대신 정규식 추출을 사용할 수 있다. JSONB 전환 시 위 쿼리를 그대로 사용 가능.

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-10-26 | 초안 작성 |
