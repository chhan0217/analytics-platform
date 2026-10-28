# 시계열 추이 분석 설계

> 작성일: 2026-10-28 (소급 작성 — 구현 완료 후 문서화)

## 1. 목적

날짜별(또는 시간별)로 이벤트 수, 세션 수, 사용자 수의 변화를 추적한다.
꺾은선 차트로 시각화하여 트래픽 추이, 특정 이벤트 증감, 프로모션/캠페인 효과를 분석한다.

## 2. 의사결정

- **프로모션/캠페인 분석**: 별도 기능 대신 `property_filter`로 해결 (MEMORY 기록 참고)
- **차트 라이브러리**: recharts 채택 (React 생태계에서 가장 널리 사용, 번들 크기 적절)

## 3. API 설계

### `GET /admin/projects/{projectId}/analytics/trend`

#### 쿼리 파라미터

| 파라미터 | 필수 | 타입 | 설명 | 기본값 |
|---------|------|------|------|--------|
| `start_date` | O | ISO-8601 | 조회 시작일 | - |
| `end_date` | O | ISO-8601 | 조회 종료일 | - |
| `granularity` | X | string | `daily` / `hourly` | `daily` |
| `event_key` | X | string | 특정 이벤트만 필터 | 전체 |
| `property_name` | X | string | 속성 필터 키 (`property_value`와 함께 사용) | - |
| `property_value` | X | string | 속성 필터 값 (`property_name`과 함께 사용) | - |
| `application_id` | X | UUID | 앱 필터 | 전체 |

#### 응답

```json
{
  "data": {
    "granularity": "daily",
    "data_points": [
      {
        "date": "2026-10-27",
        "event_count": 49,
        "session_count": 6,
        "user_count": 1
      },
      {
        "date": "2026-10-28",
        "event_count": 29,
        "session_count": 4,
        "user_count": 2
      }
    ],
    "period": {
      "start_date": "2026-10-27T00:00:00Z",
      "end_date": "2026-10-28T23:59:59Z"
    }
  }
}
```

hourly 모드일 때 `date` 형식: `"2026-10-27T09"` (시간 단위)

## 4. 계산 로직

1. `analytics_events` 테이블에서 기간 내 이벤트 조회
2. `event_key` 필터 → SQL WHERE 조건으로 처리
3. `property_name/value` 필터 → properties JSON에서 파싱 후 필터링
4. 이벤트를 날짜(또는 시간) 단위로 그룹핑
5. 각 그룹에서 집계:
   - `event_count`: 이벤트 건수
   - `session_count`: 고유 session_id 수
   - `user_count`: 고유 anonymous_id 수
6. 데이터 없는 날짜/시간도 0으로 채워서 반환 (차트 빈 구간 방지)

## 5. 활용 시나리오

| 시나리오 | 파라미터 |
|---------|---------|
| 전체 트래픽 추이 | 필터 없이 조회 |
| screen_view 추이만 | `event_key=screen_view` |
| 프로모션 효과 분석 | `event_key=purchase_completed&property_name=campaign&property_value=summer_sale` |
| 트래픽 급증 시간대 분석 | `granularity=hourly` |

## 6. 대시보드

- 경로: `/trend` (사이드바 "시계열 추이")
- recharts `LineChart` 사용
- 메트릭 전환: 이벤트 / 세션 / 사용자
- 필터 UI: 날짜 범위, granularity, event_key, property 필터

## 7. 변경 파일

| 파일 | 변경 |
|------|------|
| `backend/.../AnalyticsDto.kt` | `TrendQuery`, `TrendResponse`, `TrendDataPoint` |
| `backend/.../AnalyticsService.kt` | `trend()`, `generateHourRange()` |
| `backend/.../AnalyticsRoute.kt` | `GET /trend` 엔드포인트 |
| `web-dashboard/.../TrendPage.tsx` | 꺾은선 차트 페이지 |
| `web-dashboard/src/App.tsx` | `/trend` 라우트 |
| `web-dashboard/src/components/Layout.tsx` | 사이드바 메뉴 |
| `web-dashboard/package.json` | recharts 추가 |
| `docs/api.md` | trend API 스펙 |
