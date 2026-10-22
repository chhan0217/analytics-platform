# Day 05 — Phase 5: Analytics

> 시작일: 2026-10-22

## 체크리스트

- [x] **Step 1.** Overview API + 대시보드
  - [x] GET /admin/projects/{projectId}/analytics/overview
  - [x] 이벤트 수, 세션 수, 사용자 수 (기간/application 필터)
  - [x] 대시보드 개요 탭 (카드 UI)

- [x] **Step 2.** Screen Analytics API + 대시보드
  - [x] GET /admin/projects/{projectId}/analytics/screens
  - [x] screen_id별 방문 수, 고유 사용자 수
  - [x] 대시보드 화면 통계 탭 (테이블)

- [x] **Step 3.** Journey API + 대시보드
  - [x] GET /admin/projects/{projectId}/analytics/journeys
  - [x] 세션 내 화면 전이 집계 (A → B 횟수)
  - [x] 대시보드 여정 탭 (테이블)

- [x] **Step 4.** Funnel API + 대시보드
  - [x] POST /admin/projects/{projectId}/analytics/funnels/query
  - [x] 단계별 고유 사용자/세션, 전환율 계산
  - [x] 대시보드 퍼널 탭 (단계별 바 + 전환율)

## 완료 조건
- 4개 분석 API 동작 + 대시보드에서 시각화
- 기간 필터 동작
- 클린 빌드 통과
- CI 통과
