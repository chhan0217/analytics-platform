# 리텐션 분석 설계

> 작성일: 2026-10-27

## 1. 개요

사용자가 앱을 지속적으로 사용하는지 측정하는 리텐션(Retention) 분석 API.
두 가지 타입을 지원한다.

| 타입 | 코호트 기준 | 용도 |
|------|------------|------|
| `new` | 해당 기간 내 **첫 이벤트 발생일** | 신규 유입 품질, 온보딩 효과 측정 |
| `active` | 해당 날짜에 **이벤트가 있는 모든 사용자** | 전체 사용자 재방문 패턴, 이탈 감지 |

## 2. 핵심 개념

### 코호트(Cohort)
같은 기준 날짜를 공유하는 사용자 그룹. 타입에 따라 소속 방식이 다르다.

### D-N 리텐션
코호트 날짜로부터 N일째에 다시 이벤트가 1건이라도 있으면 "재방문"으로 판정한다.
이벤트 종류는 무관 — `screen_view`, `button_click` 등 아무 이벤트든 활동으로 인정.

### 타입별 코호트 소속 방식

두 타입은 "신규 → 기존 전환" 관계가 아니라, **같은 데이터를 다른 관점으로 보는 것**이다.

예시 — 사용자 A가 10/01 첫 방문, 10/02 재방문, 10/08 재방문한 경우:

| 타입 | 코호트 소속 | 설명 |
|------|------------|------|
| `new` | 10/01 코호트**만** | 첫 방문일에 1번만 소속. "신규 유입이 얼마나 남는가" |
| `active` | 10/01, 10/02, 10/08 코호트 **모두** | 활동한 날마다 소속. "오늘 쓴 사람이 다시 오는가" |

### 재방문 판정 기준
- `analytics_events.occurred_at`의 UTC 날짜 기준
- 코호트 날짜 + N일에 해당 사용자의 이벤트가 1건 이상 존재하면 재방문

## 3. API 스펙

### `GET /admin/projects/{projectId}/analytics/retention`

#### Query Parameters

| 파라미터 | 필수 | 설명 | 예시 |
|---------|------|------|------|
| `start_date` | O | 코호트 시작 날짜 | `2026-10-01` |
| `end_date` | O | 코호트 종료 날짜 | `2026-10-14` |
| `retention_days` | X | 측정할 D-N 목록 (기본: `1,7,30`) | `1,3,7,14,30` |
| `type` | X | `new` / `active` (기본: `new`) | `new` |
| `application_id` | X | 특정 앱 필터 | UUID |

#### 성공 응답

```json
{
  "data": {
    "type": "new",
    "retention_days": [1, 7, 30],
    "cohorts": [
      {
        "date": "2026-10-01",
        "cohort_size": 100,
        "retention": {
          "1": { "count": 40, "rate": 0.40 },
          "7": { "count": 20, "rate": 0.20 },
          "30": { "count": 10, "rate": 0.10 }
        }
      },
      {
        "date": "2026-10-02",
        "cohort_size": 80,
        "retention": {
          "1": { "count": 32, "rate": 0.40 },
          "7": { "count": 16, "rate": 0.20 }
        }
      }
    ],
    "period": {
      "start_date": "2026-10-01T00:00:00Z",
      "end_date": "2026-10-14T00:00:00Z"
    }
  }
}
```

- D-N 데이터가 아직 없는 코호트(미래 날짜)는 해당 키를 생략한다.
- `rate`는 0.0 ~ 1.0 (소수점 4자리까지).

## 4. 사용자 식별

`identity_links` 테이블에서 `external_user_id`가 연결된 사용자는 `analytics_user_id` 기준으로 그룹핑한다. 미연결 사용자는 `anonymous_id` 기준.

구현 단순화를 위해 1차에서는 `anonymous_id` 기준으로 구현하고, 이후 `analytics_user_id` 기반 병합을 추가한다.

## 5. 쿼리 로직

### type=new (신규 사용자 리텐션)

```sql
-- 1단계: 사용자별 첫 이벤트 날짜 (코호트 결정)
WITH first_seen AS (
  SELECT anonymous_id, DATE(MIN(occurred_at)) AS cohort_date
  FROM analytics_events
  WHERE project_id = :projectId
    AND occurred_at >= :start AND occurred_at < :end + interval '1 day' + max(retention_days)
  GROUP BY anonymous_id
  HAVING DATE(MIN(occurred_at)) >= :start AND DATE(MIN(occurred_at)) <= :end
),
-- 2단계: 사용자별 활동 날짜 목록
user_activity AS (
  SELECT DISTINCT anonymous_id, DATE(occurred_at) AS activity_date
  FROM analytics_events
  WHERE project_id = :projectId
)
-- 3단계: 코호트별 D-N 재방문 카운트
SELECT
  f.cohort_date,
  COUNT(DISTINCT f.anonymous_id) AS cohort_size,
  :retentionDay AS retention_day,
  COUNT(DISTINCT a.anonymous_id) AS retained_count
FROM first_seen f
LEFT JOIN user_activity a
  ON f.anonymous_id = a.anonymous_id
  AND a.activity_date = f.cohort_date + :retentionDay
GROUP BY f.cohort_date
ORDER BY f.cohort_date;
```

### type=active (활성 사용자 리텐션)

```sql
-- 1단계: 해당 날짜에 활동한 사용자 (코호트 = 활동일)
WITH daily_users AS (
  SELECT DISTINCT anonymous_id, DATE(occurred_at) AS cohort_date
  FROM analytics_events
  WHERE project_id = :projectId
    AND DATE(occurred_at) >= :start AND DATE(occurred_at) <= :end
),
user_activity AS (
  SELECT DISTINCT anonymous_id, DATE(occurred_at) AS activity_date
  FROM analytics_events
  WHERE project_id = :projectId
)
SELECT
  d.cohort_date,
  COUNT(DISTINCT d.anonymous_id) AS cohort_size,
  :retentionDay AS retention_day,
  COUNT(DISTINCT a.anonymous_id) AS retained_count
FROM daily_users d
LEFT JOIN user_activity a
  ON d.anonymous_id = a.anonymous_id
  AND a.activity_date = d.cohort_date + :retentionDay
GROUP BY d.cohort_date
ORDER BY d.cohort_date;
```

## 6. Exposed ORM 구현 방향

Exposed의 `exec`를 사용해 raw SQL로 구현한다. 코호트 + 여러 retention_days를 한 번에 처리하면 쿼리 수가 줄어들지만, 초기에는 retention_day별로 쿼리를 나누는 단순한 방식으로 시작한다.

## 7. 대시보드

### 컴포넌트
- 타입 선택: `new` / `active` 토글
- retention_days 입력 (기본: 1, 7, 30)
- 날짜 범위 선택
- 코호트 히트맵 테이블

### 히트맵 테이블
```
코호트       신규     D1      D7       D30
2026-10-01  100명   40%     20%      10%
2026-10-02   80명   35%     18%        -
2026-10-03   90명   42%       -        -
```

- 셀 배경색: 리텐션율에 따라 연한 노랑(낮음) → 진한 초록(높음)
- 아직 도달하지 않은 D-N은 `-`로 표시

## 8. 라우트/메뉴
- 경로: `/retention`
- 사이드바: 분석 그룹에 "리텐션" 추가 (여정 아래)
