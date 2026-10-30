# AGENTS.md — Universal Analytics Platform

> 이 파일은 저장소 루트의 코딩 에이전트 실행 규칙이다. Codex, Cursor 등은 작업 전에 읽고 준수한다.

## 0. 목표 및 범위

특정 서비스에 종속되지 않는 범용 고객 행동 분석 플랫폼의 **로컬 실행 가능한 프로토타입**을 구현한다. 전체 기능을 로컬에서 완성·검증한 다음 운영 서버에 배포한다. 구현 순서는 계약/인프라 → Backend/DB → KMP Android SDK → Web SDK → 관리자 웹 → 통합 분석/테스트다.

**이번 범위**: 멀티 조직/프로젝트/앱 등록, 플랫폼(Android/iOS/Web) 구분, 텍소노미 관리, 앱 이벤트 태깅, 세션 및 사용자 식별, 이벤트 수집/재전송, 이벤트 조회, 사용자별 검색, 화면별 통계, 고객 여정, 퍼널/이탈 분석, 로컬 통합 테스트.

**이번 범위 밖**: Session Replay, 실제 화면 녹화, UI 재현, 실시간 스트리밍 분석, iOS 샘플 앱 완성, Crash SDK 구축, 과도한 마이크로서비스 분할. 단, 추후 기능 추가를 막는 설계는 피한다.

## 1. 절대 설계 규칙

1. **범용성**: 서버/SDK에 채비·충전소·결제 등 특정 서비스의 도메인 이벤트를 하드코딩하지 않는다. 예시 데이터에서만 사용한다.
2. **N개 서비스**: Organization(조직) → Project(서비스) → Application(플랫폼별 클라이언트) 계층. Project마다 Android/iOS/Web Application을 각각 0개 이상 등록할 수 있고 같은 플랫폼의 앱도 여러 개 가능하다.
3. **명시적 플랫폼**: `ANDROID`, `IOS`, `WEB` enum. 프로젝트 통합/플랫폼별/개별 Application별 조회를 모두 지원한다. `application_id`가 권위 있는 플랫폼 출처이며 이벤트의 platform은 서버에서 확정한다.
4. **사용자 식별 범위**: 동일 Project의 로그인 계정은 플랫폼을 넘어 연결할 수 있다. 다른 Project의 같은 external_user_id는 동일인으로 취급하지 않는다. 익명 ID는 설치/브라우저 인스턴스 범위로 관리한다.
5. **테넌트 격리**: 모든 관리자 조회/변경은 조직·프로젝트 권한 검증을 거친다. 수집 키로 소속 Application을 판별하고 클라이언트가 전달한 임의 project_id를 신뢰하지 않는다.
6. **보안**: 앱에 내장되는 ingestion key는 공개 식별용 키로 취급하며 관리자 권한에 사용하지 않는다. 수집 API는 키 검증, 제한, payload 크기 제한, 허용 이벤트 검증, 최소 수집 정책을 적용한다. 실제 운영 전 인증·접근제어·TLS·비밀 관리 필수.
7. **로컬 우선/배포 가능성**: 모든 초기 개발·디버깅·통합 검증은 로컬 MacBook에서 수행한다. Docker Compose는 로컬 PostgreSQL 인프라와 향후 배포 재현성을 위해 사용한다. 개발 중 Ktor는 로컬 JVM, React는 로컬 Vite 프로세스로 실행한다. URL/키/DB/환경값은 설정으로 분리한다.
8. **이벤트 계약 우선**: 앱, 서버, 웹이 동일한 이벤트 스키마·버전 규칙을 따른다. 이벤트 ID는 안정적으로 유지하고 표시명/설명은 변경 가능하다.
9. **정확한 분석 의미**: 사용자 수, 세션 수, 이벤트 수, 전환율, 이탈률의 분모·시간 범위·중복 처리 규칙을 API 및 UI에 명시한다. 실제 사용자와 가상 테스트 데이터를 혼동하지 않는다.
10. **개인정보 최소화**: 이름·이메일·전화번호·토큰·비밀번호·결제정보·입력 내용·정밀 위치를 기본 수집하지 않는다. 사용자 검색은 서비스 내부 ID 중심. 관리자 접근 감사 로그 및 삭제/보존 정책을 준비한다.

## 2. 기술 스택 (무단 교체 금지)

- Backend: Kotlin, Ktor, Gradle Kotlin DSL, kotlinx.serialization, Exposed, Flyway
- Database: PostgreSQL (JSONB, index), Docker Compose
- Web Dashboard: React, TypeScript, Vite, React Router, TanStack Query, Apache ECharts
- Mobile SDK: Kotlin Multiplatform, `commonMain` 중심, Ktor Client, kotlinx.coroutines, kotlinx.serialization, SQLDelight
- Android adapter: Jetpack Compose + Navigation 연동 모듈; Android Sample App
- Web SDK: TypeScript 패키지; Web Sample App
- Testing: Kotlin unit/integration tests, DB integration tests, TypeScript unit tests, 핵심 E2E smoke tests
- 초기에는 단일 Ktor 서비스/단일 PostgreSQL DB를 사용한다. ClickHouse/Kafka/Redis는 성능 필요성이 입증되기 전 도입하지 않는다.

## 3. Monorepo 목표 구조

```text
analytics-platform/
├── AGENTS.md
├── README.md
├── docs/
│   ├── architecture.md
│   ├── event-contract.md
│   ├── taxonomy.md
│   ├── identity-and-sessions.md
│   ├── api.md
│   └── decisions/
├── backend/
│   ├── src/main/
│   └── src/test/
├── web-dashboard/
│   └── src/
├── sdk/
│   ├── analytics-kmp/       # commonMain/androidMain/iosMain
│   ├── analytics-compose/   # 선택적 Android UI 어댑터
│   └── analytics-web/       # TypeScript SDK
├── samples/
│   ├── android-app/
│   └── web-app/
├── infra/
│   ├── docker-compose.yml
│   └── .env.example
└── scripts/
```

## 4. 도메인 및 데이터 모델

필수 엔티티(명명은 일관되게 유지):
- `organizations(id, name, created_at)`
- `projects(id, organization_id, name, created_at)`
- `applications(id, project_id, name, platform, app_identifier, created_at)`
- `app_credentials(id, application_id, key_hash, status, created_at, revoked_at)`
- `admin_users`, `project_memberships` (관리자 인증/역할 및 접근 범위)
- `taxonomy_events(id, project_id, event_key, display_name, description, schema_version, status, created_at)`
- `taxonomy_properties(id, taxonomy_event_id, property_key, value_type, required, description)`
- `analytics_users(id, project_id, external_user_id_nullable, created_at, updated_at)`
- `anonymous_identities(id, project_id, anonymous_id, created_at)`
- `identity_links(id, project_id, analytics_user_id, anonymous_identity_id, linked_at, unlinked_at)`
- `analytics_sessions(id, project_id, application_id, anonymous_id, analytics_user_id_nullable, started_at, last_event_at, ended_at_nullable)`
- `analytics_events(id, project_id, application_id, session_id, analytics_user_id_nullable, anonymous_id, event_key, event_schema_version, properties_jsonb, occurred_at, received_at, sequence_number_nullable)`
- `admin_audit_logs(id, admin_user_id, action, target_type, target_id, occurred_at)`

필수 인덱스/제약:
- Project 내 `(project_id, external_user_id)` 유일성(로그인 사용자 ID가 존재할 때)
- `(project_id, event_key)` 또는 이벤트 버전 관리 정책에 맞는 taxonomy 유일성
- `(project_id, occurred_at)`, `(application_id, occurred_at)`, `(project_id, session_id, occurred_at)`, `(project_id, analytics_user_id, occurred_at)`
- event ID 전역 또는 Application별 중복 방지; 재전송 시 idempotent ingest
- DB FK와 project/application 관계를 서버에서 검증

테이블은 Flyway migration으로만 변경한다. 개발 중 임의 수동 DB 수정 금지.

## 5. 이벤트 계약 및 텍소노미

공통 기본 이벤트: `app_open`, `screen_view`, `button_click`, `session_start`, `session_end`, `identify`(식별 처리), `custom_event`는 선택적으로 제공. 이벤트 키는 영문 snake_case를 권장하고 앱별 이벤트(`product_view`, `purchase_completed`)를 자유롭게 등록한다.

필수 이벤트 필드:
```json
{
  "event_id": "uuid",
  "session_id": "uuid",
  "anonymous_id": "uuid",
  "event_key": "screen_view",
  "schema_version": 1,
  "occurred_at": "2026-10-22T10:00:00Z",
  "sequence_number": 1,
  "properties": {"screen_id": "home"},
  "context": {"app_version": "1.0.0", "sdk_version": "0.1.0", "os_version": "..."}
}
```
`application_id`, `project_id`, `platform`은 ingestion key에서 서버가 결정한다. 이벤트 배치 요청에는 배열과 SDK 메타데이터를 담는다. ISO-8601 UTC 저장, 대시보드에서 타임존 변환. `received_at`과 `occurred_at` 분리. event_id 기준 중복 저장 방지.

텍소노미 정책: 공통 시스템 이벤트와 Project별 커스텀 이벤트를 구분한다. 스키마 필수 속성/타입을 서버에서 검증한다. 미등록 이벤트 정책(거부/격리/개발 모드 허용)은 설정 가능하게 설계하되 프로토타입 기본값은 명시적으로 정한다. 운영 중 이미 수집된 이벤트 키를 무분별하게 변경하지 않는다.

## 6. 세션 및 사용자 ID 정책

- SDK 초기화 시 익명 ID를 생성하고 영속화한다. 삭제/재설치/브라우저 저장소 초기화 시 변경될 수 있다.
- `identify(externalUserId)`는 현재 Project 범위에서 로그인 계정을 연결한다.
- `resetIdentity()`는 로그아웃 후 이후 이벤트에 이전 로그인 ID가 포함되지 않도록 한다. 익명 ID 회전 정책을 문서화한다.
- 로그인 전 이벤트를 로그인 계정에 자동 소급 귀속하지 않는다. 명시적 병합 정책과 계정 전환 보호 장치가 생긴 뒤에만 수행한다.
- 세션은 앱/브라우저 활동에 따라 시작·유지·종료한다. 백그라운드 전환만으로 즉시 종료하지 않고 비활동 타임아웃(기본 30분, 설정 가능)을 사용한다.
- `session_end` 전송은 best-effort. 강제 종료/크래시/네트워크 단절 시 서버가 마지막 활동 시간으로 만료를 판정할 수 있어야 한다.
- 프로젝트 단위 로그인 사용자 통합은 지원하지만 플랫폼별 세션은 Application에 귀속한다.

## 7. SDK 계약

KMP 공통 Core API 예시:
```kotlin
Analytics.initialize(AnalyticsConfig(apiKey = "PUBLIC_INGEST_KEY", baseUrl = "...", environment = "development"))
Analytics.track("product_view", mapOf("product_id" to "p-1"))
Analytics.trackScreen("product_detail")
Analytics.identify("user_1024")
Analytics.setUserProperties(mapOf("membership_tier" to "premium"))
Analytics.resetIdentity()
Analytics.flush()
```

- 공통 이벤트 모델/세션/큐/전송/재시도는 `commonMain`; 플랫폼별 저장소 및 앱 수명주기 연결은 adapter/actual 구현으로 격리한다.
- 이벤트를 로컬에 먼저 저장한 뒤 배치 전송; 재시도 지수 백오프, 최대 큐 크기, 전송 제한, 영구 실패 정책을 명시한다.
- 네트워크 요청을 UI 메인 스레드에서 블로킹하지 않는다.
- Compose 화면 추적은 별도 모듈에서 Navigation과 연동; `Modifier.testTag`만으로 클릭 이벤트가 자동 수집된다고 가정하지 않는다. 명시적 클릭 추적 API/컴포넌트 래퍼 제공.
- iOS target을 KMP 프로젝트에 선언하되 1차 수용 테스트는 Android 중심. iOS에서 공통 코드가 컴파일되는지 가능 범위에서 확인한다.
- Web SDK는 동일 이벤트 계약과 identify/reset/track/trackScreen/flush API를 TypeScript로 구현한다.

## 8. Backend API (초안, `/api/v1`)

수집:
- `POST /ingest/events` : Application ingestion key로 배치 이벤트 수집, 각 event_id 중복 처리

관리자 인증 및 관리:
- `POST /admin/auth/login` 또는 명확한 로컬 개발 인증 흐름 (운영 배포 전 안전한 인증 구현 필수)
- `GET/POST /admin/organizations`
- `GET/POST /admin/projects`
- `GET/POST /admin/projects/{projectId}/applications`
- `GET/POST/PATCH /admin/projects/{projectId}/taxonomy/events`
- `GET /admin/projects/{projectId}/events` (기간, 플랫폼, application, event_key, user, session 필터 + 페이지네이션)
- `GET /admin/projects/{projectId}/users` (external_user_id / anonymous_id 검색)
- `GET /admin/projects/{projectId}/users/{userId}/sessions`
- `GET /admin/projects/{projectId}/sessions/{sessionId}/events`
- `GET /admin/projects/{projectId}/analytics/overview`
- `GET /admin/projects/{projectId}/analytics/screens`
- `GET /admin/projects/{projectId}/analytics/journeys`
- `POST /admin/projects/{projectId}/analytics/funnels/query`

명확한 DTO, 입력 검증, 오류 코드, 페이지네이션, 시간 필터, 권한 체크, OpenAPI 문서를 제공한다. API 이름은 구현 전에 docs/api.md에서 최종 확정한다.

## 9. 웹 관리자 기능

1. 조직/프로젝트/앱 등록 및 선택
2. 앱 플랫폼(전체/Android/iOS/Web), 개별 앱, 앱 버전, 기간 필터
3. 텍소노미 이벤트/속성 등록·수정·조회·상태 관리
4. 이벤트 원본 조회, 검색, 상세 JSON 보기
5. 사용자 검색: external_user_id 및 anonymous_id; 사용자별 세션 목록 및 이벤트 타임라인
6. Overview: 이벤트 수, 세션 수, 사용자 수, 화면 방문 수
7. Screen Analytics: screen_id별 방문 수/고유 사용자 수
8. Journey: 화면 전이 집계(예: 홈→검색→상세); 프로젝트/플랫폼 필터
9. Funnel: 단계별 고유 사용자/세션 전환 및 이탈률, 지정 기간 및 단계 순서 지원

대시보드 예시 숫자는 반드시 mock/demo로 표기한다. 실데이터와 혼합 금지.

## 10. Local-First Development Rules (필수)

- **모든 초기 개발, 디버깅, 테스트, 통합 검증은 로컬 MacBook에서만 수행한다.** 프로토타입의 핵심 기능을 완성하고 로컬 E2E 검증을 통과하기 전에는 클라우드 서버 배포를 진행하지 않는다.
- **Docker Compose는 로컬 인프라 구성 및 향후 운영 배포 재현성을 위해 사용한다.** Docker Compose를 사용한다고 해서 개발 단계부터 모든 앱을 컨테이너에서 실행해야 하는 것은 아니다.
- **PostgreSQL은 로컬 Docker Compose로 실행한다.** 데이터는 Docker volume에 보존하고 Flyway migration으로 스키마를 관리한다.
- **Ktor Backend는 개발 중 Mac 로컬 JVM 프로세스로 실행한다.** IDE 실행/디버깅과 Gradle 실행을 모두 지원한다. 기본 개발 주소 예시: `http://localhost:8080`.
- **React Web Dashboard는 로컬 Vite 개발 서버로 실행한다.** 기본 개발 주소 예시: `http://localhost:5173`. API 연동을 위해 개발 프록시 또는 환경변수 기반 API URL을 사용한다.
- **KMP SDK는 Android Studio에서 개발·테스트한다.** Android Emulator 및 실제 기기에서 로컬 Ktor 서버로 이벤트를 전송해 검증한다. iOS 타깃 구조는 유지하되 초기 기능 검증은 Android 우선이다.
- **Web SDK는 로컬 Web Sample App에서 검증한다.** 모든 수집 이벤트는 로컬 Backend 및 PostgreSQL로 전송한다.
- 초기에는 클라우드 DB, 외부 호스팅, 운영 API를 사용하지 않는다. 외부 서비스 연동이나 서버 배포는 사용자가 명시적으로 요청한 이후에만 진행한다.
- Backend `Dockerfile` 및 운영용 Compose 설정은 배포 준비용으로 별도 작성할 수 있으나, 로컬 개발 기본 실행 방식은 JVM + Vite + Docker PostgreSQL로 유지한다.
- 운영 환경 전환 시 애플리케이션 코드를 수정하지 않고 환경 변수·설정·배포 구성만 변경할 수 있도록 설계한다. 운영 전 TLS, 관리자 인증/권한, 키 관리, 데이터 백업 및 보관 정책을 검증한다.
- 로컬 개발과 운영 배포는 서로 다른 환경 파일/설정을 사용하며, 실제 `.env`와 비밀 키를 Git에 커밋하지 않는다.

### 로컬 실행 구성

| 구성 요소 | 프로토타입 실행 방식 |
|---|---|
| PostgreSQL | 로컬 Docker Compose (`localhost:5432`) |
| Ktor Backend | Mac 로컬 JVM (`localhost:8080`) |
| React Dashboard | 로컬 Vite (`localhost:5173`) |
| KMP SDK / Android Sample | Android Studio + Emulator/실기기 |
| Web SDK / Web Sample | 로컬 브라우저 + Vite |
| 운영 서버 | 로컬 기능 완성·검증 후 별도 배포 |

### 로컬 실행 및 검증 상세

- Android Emulator에서 호스트 접근은 보통 `http://10.0.2.2:8080`; 실제 기기는 LAN IP 필요. 개발용 HTTP 예외는 debug 전용, release는 HTTPS.
- `.env.example` 제공, 실제 `.env`/비밀 키는 gitignore.
- README에 **사전 준비, 설치, 실행 명령, 포트, 샘플 앱 테스트, 초기 관리자 설정, API 확인, 테스트 명령, 트러블슈팅**을 명시.
- 앱/서버 로그에는 인증 키 및 개인 식별값을 불필요하게 출력하지 않는다.

## 11. 구현 단계 및 완료 조건

### Phase 1 — Scaffold & contracts
- 개발 중에는 PostgreSQL만 Docker Compose로 실행하고 Ktor/Vite는 로컬 프로세스로 실행하는 기본 구성 확정
- Monorepo, Gradle, Vite, KMP, Docker Compose, .env.example, README 생성
- docs에 이벤트 계약/세션·ID/데이터 모델/API 설계 작성
- Docker PostgreSQL 기동, Ktor health endpoint, Web 홈, Android 샘플 빌드

### Phase 2 — Backend / Taxonomy
- Flyway migrations, organization/project/application/credential/taxonomy CRUD
- 프로젝트 권한 및 앱 수집 키 검증
- 이벤트 배치 수집, 검증, idempotency, 조회 API, API 테스트

### Phase 3 — SDK
- KMP Core + Android 연결, 익명 ID/세션/로컬 큐/배치 전송/재시도
- Compose 화면 및 버튼 태깅, identify/reset
- Android 샘플 앱 이벤트가 PostgreSQL에 적재됨
- Web SDK 및 웹 샘플 이벤트 적재

### Phase 4 — Web Dashboard
- 프로젝트/플랫폼/앱 선택, 텍소노미 CRUD, 이벤트 탐색
- 사용자 검색 및 세션/이벤트 타임라인

### Phase 5 — Analytics
- 화면 통계, 화면 전이 집계, 퍼널/이탈 분석
- 기간·플랫폼·앱 필터, 분모 및 중복 처리 검증

### Phase 6 — End-to-end validation
- 최소 2개 Project, 각 Android/Web 앱 및 iOS Application 등록 데이터 준비
- 동일 external_user_id의 프로젝트 간 격리 테스트
- 동일 프로젝트 Android/Web 사용자 통합 조회 테스트
- 중복 event_id 재전송, 네트워크 장애, 앱 재시작 후 큐 재전송 테스트
- 테스트 명령 및 결과를 README에 기록
- 로컬 PostgreSQL + 로컬 Ktor + 로컬 Vite + Android Emulator/Web Sample 통합 테스트를 모두 통과한 후에만 운영 배포 준비 단계로 전환

각 Phase 종료 시 **빌드/테스트 실행, 실패 원인 수정, README 갱신, 변경 파일 및 남은 이슈 요약**. 다음 단계로 넘어가기 전에 해당 단계 완료 조건을 확인한다. 외부 계정/서버 배포는 사용자 승인 없이 진행하지 않는다.

## 12. CI/CD

### 플랫폼
- **GitHub Actions** — 모든 CI/CD 파이프라인을 GitHub Actions로 구성한다.

### CI (Phase 1부터 적용)

PR 생성 및 push 시 변경된 모듈만 자동으로 빌드/테스트를 실행한다 (path filter).

| 모듈 | 트리거 경로 | CI 작업 |
|---|---|---|
| Backend | `backend/**` | Gradle build + test |
| KMP SDK | `sdk/analytics-kmp/**`, `sdk/analytics-compose/**` | Gradle build + test |
| Web Dashboard | `web-dashboard/**` | npm ci + build + test |
| Web SDK | `sdk/analytics-web/**` | npm ci + build + test |

- 각 모듈은 독립적으로 빌드/테스트 가능해야 한다.
- CI가 실패한 PR은 머지하지 않는다.

### CD (로컬 통합 검증 완료 후 적용)

Phase 6 로컬 E2E 검증을 통과한 후에 배포 파이프라인을 구성한다. 그 전에는 CD를 설정하지 않는다.

**배포 대상:**

| 구성 요소 | 배포 대상 | 방식 |
|---|---|---|
| Backend (Ktor) | AWS ECS Fargate | Docker 이미지 → ECR push → ECS 배포 |
| PostgreSQL | AWS RDS PostgreSQL | 로컬 Docker → RDS 전환 (환경변수) |
| Web Dashboard | AWS S3 + CloudFront | npm build → S3 업로드 → CloudFront 무효화 |
| KMP SDK | Maven Central | Sonatype staging → release |
| Web SDK | npm | npm publish |

**배포 흐름:**

```
main 머지 → staging 환경 자동 배포
  ├── Backend: Docker 빌드 → ECR → ECS (staging)
  ├── Web Dashboard: build → S3 (staging)
  └── DB migration: ECS task로 Flyway 실행

수동 승인 → production 환경 배포

태그(vX.Y.Z) 생성 → SDK 퍼블리시
  ├── KMP SDK → Maven Central
  └── Web SDK → npm
```

**환경 분리:**
- `staging` / `production` 환경은 별도 AWS 리소스 및 환경 변수를 사용한다.
- 인프라 구성은 `infra/` 디렉토리에 관리한다.
- 실제 `.env`, 시크릿, AWS 키는 GitHub Secrets로 관리하고 Git에 커밋하지 않는다.

**SDK 퍼블리시 정책:**
- API가 안정된 후에 퍼블리시한다. Breaking change가 빈번한 단계에서는 공개하지 않는다.
- 버전은 Semantic Versioning(semver)을 따른다.
- 퍼블리시는 Git 태그 생성으로만 트리거한다.

## 13. 에이전트 작업 방식

- 코드를 작성하기 전에 기존 파일과 이 문서를 확인한다.
- 대규모 구현은 Phase별로 나눠 진행하되, 각 Phase는 실행 가능한 상태로 남긴다.
- 불명확한 부분은 합리적인 기본값을 정해 docs/decisions에 기록하고, 보안·개인정보·파괴적 변경은 확인을 요청한다.
- 기능을 '구현 완료'로 보고하기 전 실제 빌드/테스트를 실행한다. 미실행 테스트는 통과했다고 쓰지 않는다.
- 가짜 성공 응답이나 하드코딩된 대시보드 데이터로 실제 동작을 대체하지 않는다.
- 신규 의존성은 필요한 이유를 설명하고 버전 호환성을 확인한다.
- 작업 완료 시: 변경 내용 / 실행 방법 / 실행한 검증과 결과 / 알려진 제한 / 다음 단계 제안 순서로 요약한다.
