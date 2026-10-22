# Day 06 — Phase 6: E2E Validation

> 시작일: 2026-10-22

## 체크리스트

- [x] **Step 1.** 멀티 프로젝트 격리 테스트
  - [x] 2개 프로젝트 생성, 각각 Android/Web 앱 등록
  - [x] 동일 external_user_id가 프로젝트 간 격리되는지 검증
  - [x] 프로젝트 A의 이벤트가 프로젝트 B에 노출되지 않는지 검증

- [x] **Step 2.** 크로스 플랫폼 사용자 통합 테스트
  - [x] 같은 프로젝트의 Android/Web 앱에서 동일 external_user_id로 identify
  - [x] 사용자 조회 시 두 플랫폼 세션이 모두 보이는지 검증

- [x] **Step 3.** 수집 idempotency + 검증 테스트
  - [x] 동일 event_id 재전송 시 중복 저장 안 됨
  - [x] ARCHIVED 이벤트 수집 거부
  - [x] 잘못된 키, 빈 배치, 초과 배치 등 에러 케이스

- [x] **Step 4.** 분석 API 통합 테스트
  - [x] Overview 수치 정확성
  - [x] Screen 통계 정확성
  - [x] Funnel 전환율 계산 정확성

- [x] **Step 5.** README 갱신 + 최종 정리
  - [x] 테스트 명령 및 결과 기록
  - [x] 알려진 제한사항 정리

## 구현 중 발견 및 수정한 버그

- **IngestService `external_user_id` 미처리**: 수집 DTO에 `externalUserId` 필드가 존재했지만 `processEvents()`에서 `AnalyticsUser` 생성 및 세션/이벤트 연결이 누락되어 있었다. `ensureAnalyticsUser()` 메서드를 추가하고, 세션과 이벤트에 `analyticsUserId`를 연결하도록 수정했다.

## 완료 조건
- [x] 모든 E2E 테스트 통과 (61개)
- [x] CI 통과 (`./gradlew :backend:clean :backend:build` 성공)
- [x] README 갱신
