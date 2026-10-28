# DAU / WAU / MAU + 스티키니스 설계

> 작성일: 2026-10-28

## 1. 목적

일간/주간/월간 활성 사용자 수(DAU/WAU/MAU)와 스티키니스(DAU/MAU 비율)를 제공한다.
Overview 페이지에 핵심 KPI로 추가하여 앱 성장 지표를 한눈에 파악한다.

## 2. 핵심 개념

### DAU / WAU / MAU

- **DAU**: 해당 날짜에 이벤트가 1건 이상인 고유 anonymous_id 수
- **WAU**: 최근 7일간 이벤트가 1건 이상인 고유 anonymous_id 수 (롤링 윈도우)
- **MAU**: 최근 30일간 이벤트가 1건 이상인 고유 anonymous_id 수 (롤링 윈도우)

### 스티키니스

- **스티키니스 비율**: DAU / MAU (0.0 ~ 1.0)
- **의미**: MAU 중 몇 %가 매일 접속하는가. 높을수록 습관화된 앱.

## 3. API 설계

### `GET /admin/projects/{projectId}/analytics/active-users`

#### 쿼리 파라미터

| 파라미터 | 필수 | 타입 | 설명 | 기본값 |
|---------|------|------|------|--------|
| `date` | X | 날짜 (YYYY-MM-DD) | 기준 날짜 | 오늘 (UTC) |
| `application_id` | X | UUID | 앱 필터 | 전체 |

#### 응답

```json
{
  "data": {
    "date": "2026-10-28",
    "dau": 18,
    "wau": 35,
    "mau": 80,
    "stickiness": 0.225
  }
}
```

## 4. 계산 로직

기준 날짜를 `D`라 할 때:

1. **DAU**: `D` 하루 동안 이벤트가 있는 고유 anonymous_id 수
2. **WAU**: `D-6 ~ D` (7일간) 이벤트가 있는 고유 anonymous_id 수
3. **MAU**: `D-29 ~ D` (30일간) 이벤트가 있는 고유 anonymous_id 수
4. **스티키니스**: DAU / MAU (MAU가 0이면 0.0)

날짜 기준은 `occurred_at`의 UTC 날짜 (substring 0~10).

## 5. 대시보드

Overview 페이지 상단에 기존 KPI와 함께 표시:

```
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ DAU      │ │ WAU      │ │ MAU      │ │스티키니스 │ │ 이벤트   │
│    18    │ │    35    │ │    80    │ │  22.5%   │ │  1,240   │
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
```

페이지 로드 시 자동으로 오늘 기준 값을 조회한다.

## 6. 변경 파일 (예상)

| 파일 | 변경 |
|------|------|
| `backend/.../AnalyticsDto.kt` | `ActiveUsersResponse` 추가 |
| `backend/.../AnalyticsService.kt` | `activeUsers()` 메서드 추가 |
| `backend/.../AnalyticsRoute.kt` | `GET /active-users` 엔드포인트 추가 |
| `web-dashboard/.../OverviewPage.tsx` | DAU/WAU/MAU/스티키니스 카드 추가 |
| `docs/api.md` | active-users API 스펙 추가 |
