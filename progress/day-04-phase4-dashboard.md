# Day 04 — Phase 4: Web Dashboard

> 시작일: 2026-10-22

## 체크리스트

- [x] **Step 1.** 프로젝트 구조 + API 클라이언트 + 라우팅
  - [x] React Router 설정 (BrowserRouter, 인증 가드)
  - [x] API 클라이언트 (fetch + Bearer 토큰 관리 + ApiError)
  - [x] 로그인 페이지 (admin@analytics.local / admin1234)
  - [x] 레이아웃 (사이드바 내비게이션 + 로그아웃)

- [x] **Step 2.** 조직/프로젝트/앱 관리
  - [x] 조직 목록 + 생성
  - [x] 프로젝트 목록 + 생성 (이벤트 정책 표시)
  - [x] 앱 목록 + 등록 (플랫폼 선택) + 수집 키 발급/폐기

- [x] **Step 3.** 텍소노미 관리
  - [x] 이벤트 목록 + 등록 (snake_case 키 + 표시명)
  - [x] 속성 목록 + 추가 (값 타입 + 필수 여부)
  - [x] 시스템 이벤트 구분 표시

- [x] **Step 4.** 이벤트 탐색
  - [x] 이벤트 목록 (기간 필터 + event_key 필터 + cursor 페이지네이션)
  - [x] 이벤트 상세 (properties/context JSON 보기)

- [x] **Step 5.** 사용자 검색 + 세션/이벤트 타임라인
  - [x] Backend: 사용자/세션/이벤트 조회 API 추가 (UserQueryRoute)
  - [x] 사용자 검색 (external_user_id / anonymous_id)
  - [x] 사용자별 세션 목록
  - [x] 세션별 이벤트 타임라인

## 기타
- [x] Backend CORS 설정 추가 (localhost:5173, 5174)
- [x] 대시보드 빌드 통과

## 완료 조건
- 로그인 → 프로젝트 선택 → 이벤트 조회 → 상세 보기 전체 흐름 동작
- 텍소노미 CRUD 동작
- 앱 등록 + 수집 키 발급 동작
- 사용자 검색 + 세션 타임라인 동작
