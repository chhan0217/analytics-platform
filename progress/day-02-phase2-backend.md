# Day 02 — Phase 2: Backend / Taxonomy

> 시작일: 2026-10-22

## 체크리스트

- [x] **Step 1.** Flyway 설정 + DB 마이그레이션
  - [x] Flyway 통합 설정 (Database.kt에서 서버 시작 시 자동 실행)
  - [x] V001~V014 마이그레이션 14개 작성
  - [x] 14개 테이블 + 인덱스/제약 조건 생성 확인
  - [x] 주요 설계: analytics_events PK는 클라이언트 event_id 직접 사용 (idempotency), projects에 unregistered_event_policy 추가, taxonomy_events에 is_system 플래그 추가, app_credentials에 key_prefix 저장

- [x] **Step 2.** Organization CRUD
  - [x] Exposed 테이블 정의 (OrganizationTable)
  - [x] Repository + Service + DTO + Route
  - [x] 공통 응답 구조 (DataResponse, ListResponse, PageMeta)
  - [x] 테스트 유틸 (createTestClient, setupTestApp, cleanDatabase)
  - [x] 통합 테스트 6개 전부 통과

- [x] **Step 3.** Project CRUD
  - [x] Exposed 테이블 정의 (ProjectTable)
  - [x] Repository + Service + DTO + Route
  - [x] Organization 소속 검증 (존재하지 않는 조직 → 404)
  - [x] unregisteredEventPolicy 검증 (PERMISSIVE/STRICT/QUARANTINE)
  - [x] 통합 테스트 6개 전부 통과 (누적 12개)

- [x] **Step 4.** Application CRUD
  - [x] Platform enum (ANDROID, IOS, WEB) + Exposed 테이블
  - [x] Repository + Service + DTO + Route
  - [x] Project 소속 검증, platform 유효성 검증, platform 변경 불가
  - [x] DB 커넥션 싱글턴화 (테스트 간 too many clients 해결)
  - [x] 통합 테스트 7개 전부 통과 (누적 19개)

- [x] **Step 5.** 관리자 인증 (로컬 간소화)
  - [x] admin_users 시딩 (admin@analytics.local / admin1234)
  - [x] 로그인 API (POST /admin/auth/login → JWT 반환)
  - [x] JWT 인증 미들웨어 (admin-jwt, 관리자 API 보호)
  - [x] PasswordUtil (SHA-256, 운영 전 BCrypt 교체 필요)
  - [x] 테스트 헬퍼 (createAuthenticatedClient)
  - [x] 통합 테스트 4개 (로그인, 잘못된 비밀번호, 미인증 접근, 토큰 접근)
  - [x] 기존 테스트 전부 인증 적용 후 통과 (누적 23개)
  - [ ] project_memberships 권한 검증 (Phase 2 내 추후 적용)

- [x] **Step 6.** App Credentials (수집 키)
  - [x] IngestionKeyUtil (ink_ + 32자 crypto-random hex, SHA-256 해시)
  - [x] 발급 API (POST, 원본 1회 반환)
  - [x] 목록 API (GET, keyPrefix만 노출)
  - [x] 폐기 API (DELETE, REVOKED 상태 전환)
  - [x] 통합 테스트 4개 전부 통과 (누적 27개)

- [x] **Step 7.** Taxonomy Events CRUD
  - [x] Repository + Service + DTO + Route
  - [x] 시스템 이벤트 6개 자동 시딩 (프로젝트 생성 시 ProjectService에서 호출)
  - [x] event_key 불변성 (PATCH에서 변경 불가), snake_case 패턴 검증
  - [x] status 전이 (ACTIVE/DEPRECATED/ARCHIVED), 시스템 이벤트 ARCHIVED 금지
  - [x] 중복 event_key 등록 시 409 Conflict
  - [x] 통합 테스트 7개 전부 통과 (누적 34개)

- [x] **Step 8.** Taxonomy Properties CRUD
  - [x] Repository + Service + DTO + Route
  - [x] PropertyValueType enum 검증 (STRING, INTEGER, FLOAT, BOOLEAN, ARRAY, OBJECT)
  - [x] 중복 속성 키 409 Conflict
  - [x] 통합 테스트 5개 전부 통과 (누적 39개)

- [x] **Step 9.** 이벤트 수집 API
  - [x] POST /ingest/events 엔드포인트 (admin-jwt 밖, X-Ingestion-Key 헤더)
  - [x] Ingestion key 검증 (SHA-256 해시 → app_credentials → application → project)
  - [x] 텍소노미 검증 (PERMISSIVE: 경고 후 수집, STRICT: 거부, ARCHIVED: 거부)
  - [x] 서버 결정 필드 주입 (application_id, project_id, received_at)
  - [x] event_id idempotency (PK 중복 체크로 자연 보장)
  - [x] 페이로드 제한 (배치 100개 초과 시 413)
  - [x] 세션/익명ID 레코드 자동 생성 (ensureSession, ensureAnonymousIdentity)
  - [x] JSONB 커스텀 컬럼 타입 구현 (JsonbColumnType)
  - [x] 통합 테스트 7개 전부 통과 (누적 46개)

- [x] **Step 10.** 이벤트 조회 API
  - [x] GET /admin/projects/{projectId}/events (cursor 페이지네이션, occurred_at|id 복합 커서)
  - [x] GET /admin/projects/{projectId}/events/{eventId} (상세)
  - [x] 필터: 기간(필수), application_id, event_key, user_id, session_id
  - [x] 통합 테스트 5개 전부 통과 (누적 51개)

## 완료 조건
- 모든 마이그레이션 성공, 테이블/인덱스 생성 확인
- 관리자 API CRUD 전체 동작
- 수집 API: 정상 수집, 중복 방지, 검증 거부 동작
- 이벤트 조회 API: 필터 + 페이지네이션 동작
- 전체 통합 테스트 통과
- CI 통과

## 메모
(작업 중 결정사항, 이슈, 변경점 기록)
