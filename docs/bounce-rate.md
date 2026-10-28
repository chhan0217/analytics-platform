# 화면별 이탈률 설계

> 작성일: 2026-10-28

## 1. 목적

사용자가 특정 화면에서 **다른 화면으로 이동하지 않고 세션이 종료된 비율**을 측정한다.
이탈률이 높은 화면을 식별하여 UX 개선 포인트를 찾는다.

## 2. 핵심 개념

### 이탈(Bounce)의 정의

세션 내에서 마지막으로 본 화면 = 이탈 화면.
해당 화면 이후 다른 `screen_view`가 없이 세션이 끝났으므로, 그 화면에서 이탈한 것으로 판정한다.

```
세션 A: 홈 → 충전소 목록 → 충전소 상세 → (세션 종료)
                                ↑ 이탈 화면

세션 B: 홈 → (세션 종료)
         ↑ 이탈 화면

세션 C: 홈 → 충전소 목록 → 홈 → (세션 종료)
                            ↑ 이탈 화면
```

### 이탈률 계산

```
화면 X의 이탈률 = (화면 X에서 이탈한 세션 수) / (화면 X를 방문한 세션 수)
```

| 화면 | 방문 세션 수 | 이탈 세션 수 | 이탈률 |
|------|------------|------------|--------|
| 홈 | 100 | 20 | 20% |
| 충전소 목록 | 80 | 32 | 40% |
| 충전소 상세 | 50 | 30 | 60% |

### 주의사항

- 한 세션에서 같은 화면을 여러 번 방문할 수 있다 (홈 → 목록 → 홈). 마지막 화면만 이탈로 카운트.
- 세션의 마지막 `screen_view`를 기준으로 판정. `screen_leave`나 `session_end` 이벤트 유무와 무관.

## 3. API 설계

기존 화면 분석 API를 확장한다. 별도 엔드포인트를 만들지 않는다.

### `GET /admin/projects/{projectId}/analytics/screens`

#### 기존 응답에 `bounce_rate` 필드 추가

```json
{
  "data": {
    "screens": [
      {
        "screen_id": "home",
        "view_count": 500,
        "unique_users": 120,
        "avg_duration_ms": 15000,
        "bounce_count": 125,
        "bounce_rate": 0.25
      },
      {
        "screen_id": "station_list",
        "view_count": 350,
        "unique_users": 100,
        "avg_duration_ms": 30000,
        "bounce_count": 140,
        "bounce_rate": 0.40
      }
    ]
  }
}
```

- `bounce_count`: 이 화면에서 이탈한 세션 수
- `bounce_rate`: 이탈률 (0.0 ~ 1.0)

## 4. 계산 로직

1. 기간 내 `screen_view` 이벤트를 세션별로 그룹핑
2. 각 세션을 `occurred_at` 기준으로 정렬
3. 세션의 마지막 `screen_view`의 `screen_id` = 이탈 화면
4. 화면별 집계:
   - `view_count`: 해당 화면의 총 screen_view 수 (기존)
   - `bounce_count`: 이탈 화면으로 지정된 세션 수
   - `bounce_rate`: bounce_count / (해당 화면을 포함하는 세션 수)

### 이탈률 분모 기준

`view_count`가 아닌 **해당 화면을 방문한 세션 수**를 분모로 사용한다.
한 세션에서 같은 화면을 3번 봤어도 1세션으로 카운트.

## 5. 대시보드

기존 화면 분석 테이블(`/screens`)에 컬럼 추가:

| 화면 | 방문 수 | 고유 사용자 | 평균 체류시간 | **이탈 수** | **이탈률** |
|------|--------|-----------|-------------|-----------|----------|
| home | 500 | 120 | 15초 | 125 | 25.0% |
| station_list | 350 | 100 | 30초 | 140 | 40.0% |

## 6. 변경 파일 (예상)

| 파일 | 변경 |
|------|------|
| `backend/.../AnalyticsDto.kt` | `ScreenEntry`에 `bounceCount`, `bounceRate` 필드 추가 |
| `backend/.../AnalyticsService.kt` | `screens()` 메서드에 이탈률 계산 로직 추가 |
| `web-dashboard/.../AnalyticsPage.tsx` | 화면 분석 테이블에 이탈률 컬럼 추가 |
| `docs/api.md` | screens 응답 스펙 업데이트 |
