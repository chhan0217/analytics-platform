# CSV 내보내기 설계

> 작성일: 2026-10-28

## 1. 목적

각 분석 페이지에서 조회된 데이터를 CSV 파일로 다운로드한다.
서버 변경 없이 대시보드에서만 구현한다.

## 2. 대상 페이지 및 컬럼

| 페이지 | 파일명 | CSV 컬럼 |
|--------|--------|---------|
| 시계열 추이 | trend_{date}.csv | 날짜, 이벤트 수, 세션 수, 사용자 수 |
| 화면 분석 | screens_{date}.csv | 화면, 방문 수, 고유 사용자, 평균 체류시간(ms), 이탈 수, 이탈률 |
| 이벤트 집계 | events_{date}.csv | 이벤트 키, 횟수, 사용자 수, 세션 수 |
| 퍼널 | funnel_{date}.csv | 단계, 이벤트 키, 사용자 수, 세션 수, 전환율 |
| 리텐션 | retention_{date}.csv | 코호트 날짜, 코호트 크기, 각 D-N별 수/비율 |
| 여정 | journeys_{date}.csv | 출발 화면, 도착 화면, 전이 횟수 |

## 3. 구현 방식

- 공통 유틸 함수 `downloadCsv(filename, headers, rows)` 생성
- 이미 API로 받아온 데이터를 변환 → Blob → 다운로드
- 각 페이지 "조회" 버튼 옆에 "CSV 다운로드" 버튼 추가
- 데이터가 없으면 버튼 비활성화

## 4. 변경 파일

| 파일 | 변경 |
|------|------|
| `web-dashboard/src/utils/csv.ts` | 공통 CSV 다운로드 유틸 (신규) |
| `web-dashboard/src/features/analytics/TrendPage.tsx` | CSV 버튼 추가 |
| `web-dashboard/src/features/analytics/AnalyticsPage.tsx` | 화면/퍼널/여정 CSV 버튼 추가 |
| `web-dashboard/src/features/events/EventSummaryPage.tsx` | CSV 버튼 추가 |
| `web-dashboard/src/features/analytics/RetentionPage.tsx` | CSV 버튼 추가 |
