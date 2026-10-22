# CLAUDE.md — Development Rules

> 이 파일은 Claude Code가 매 세션마다 자동으로 읽는 개발 규칙이다.

## 1. Git 전략: Git Flow

```
main ← release/x.y.z ← develop ← feature/xxx
                                 ← fix/xxx
                                 ← hotfix/xxx
```

- `main`: 프로덕션 배포 가능한 안정 브랜치
- `develop`: 개발 통합 브랜치, 모든 feature/fix는 여기로 머지
- `feature/*`: 기능 개발 브랜치 (`develop`에서 분기, `develop`으로 머지)
- `fix/*`: 버그 수정 브랜치 (`develop`에서 분기, `develop`으로 머지)
- `release/*`: 릴리즈 준비 브랜치 (`develop`에서 분기, `main`과 `develop`으로 머지)
- `hotfix/*`: 프로덕션 긴급 수정 (`main`에서 분기, `main`과 `develop`으로 머지)

## 2. 커밋 메시지: Conventional Commits

```
<type>(<scope>): <description>
```

### Type
- `feat`: 새 기능
- `fix`: 버그 수정
- `docs`: 문서 변경
- `chore`: 빌드, 설정, 의존성 등 코드 외 변경
- `refactor`: 기능 변경 없는 코드 개선
- `test`: 테스트 추가/수정
- `style`: 포맷팅, 세미콜론 등 코드 의미 변경 없는 수정

### Scope (모듈명)
- `backend`, `sdk-kmp`, `sdk-compose`, `sdk-web`, `web-dashboard`, `infra`, `docs`

### 예시
```
feat(backend): 이벤트 배치 수집 API 추가
fix(sdk-kmp): 세션 타임아웃 계산 오류 수정
docs: 이벤트 계약 문서 작성
chore(infra): Docker Compose PostgreSQL 설정
test(backend): Organization CRUD 단위 테스트
```

## 3. PR 단위: 파일/커밋 단위 (작은 단위)

- 하나의 PR은 하나의 명확한 변경 목적만 가진다.
- PR 크기는 가능한 작게 유지한다 (리뷰 가능한 최소 단위).
- 예시:
  - `feat(backend): Organization 엔티티 및 마이그레이션` — 1 PR
  - `feat(backend): Organization CRUD API` — 1 PR
  - `test(backend): Organization CRUD 테스트` — 1 PR
- PR 제목은 Conventional Commits 형식을 따른다.
- PR 본문에 변경 내용, 테스트 방법, 관련 이슈를 명시한다.
- CI가 통과한 PR만 머지한다.

## 4. 코드 스타일

### Kotlin (Backend + KMP SDK)
- **ktlint**: 포맷팅 검사 및 자동 수정
- **detekt**: 정적 분석 (코드 스멜, 복잡도, 잠재 버그). 초기에는 룰셋을 느슨하게 시작하고 점진적으로 강화한다.

### TypeScript (Web Dashboard + Web SDK)
- **ESLint**: 린팅 (React, TypeScript 플러그인 포함)
- **Prettier**: 코드 포맷팅 (eslint-config-prettier로 충돌 방지)

### 자동화
- **커밋 훅 (husky + lint-staged)**: 커밋 전 로컬에서 포맷팅/린트 자동 실행
- **CI**: 최종 검증 (포맷팅, 린트, 정적 분석 모두 체크)

## 5. 작업 방식

- **위임 단위**: PR 단위로 작업하고 매 PR마다 사용자 확인을 받는다.
- **코드 리뷰**: 에이전트가 코드 작성 + PR 생성까지 수행한다. 머지는 사용자가 확인 후 직접 한다.
- **문서**: Design First. 구현 전에 설계 문서를 먼저 작성하고, 구현 후 변경 사항을 문서에 반영한다.

## 6. 테스트 기준

- **커버리지 목표**: 전체 80% 이상
- **범위**: 단위 테스트 + 통합 테스트 + E2E 테스트 모두 수행
- **원칙**: 모든 기능은 테스트와 함께 PR에 포함한다. 테스트 없는 기능 PR은 머지하지 않는다.
- **데이터 정합성** (이벤트 수집, 세션 관리, 사용자 식별, 중복 처리 등)은 엣지 케이스까지 커버한다.
- **E2E**: Phase 6에서 핵심 시나리오(SDK → 서버 → DB → 대시보드) 전체 흐름을 검증한다.

## 7. 언어 규칙

- **코드** (변수명, 함수명, 클래스명, 패키지명): 영어
- **주석, 커밋 메시지, PR, 문서**: 한국어

## 8. 의존성 관리

- **Gradle**: Version Catalog (`libs.versions.toml`)로 모든 Kotlin 모듈의 버전을 한 곳에서 관리한다.
- **npm**: caret(`^`) 기본값 사용 + `package-lock.json`을 Git에 커밋한다.
- **새 라이브러리 추가**: AGENTS.md에 정의된 기존 스택은 자유롭게 사용. 신규 라이브러리는 PR에 사유, 대안 검토, 라이선스를 명시하고 사용자 확인 후 추가한다.

## 9. 에러 핸들링

### Backend
- **공통 에러 응답**: `{ "success": false, "error": { "code": "...", "message": "...", "details": {} } }`. 성공 응답은 데이터를 직접 반환한다.
- **HTTP 상태 코드**: 200/201(성공), 400(입력 검증), 401(인증), 403(권한), 404(없음), 409(중복), 413(페이로드 초과), 500(서버 오류)
- **Kotlin 패턴**: 커스텀 `ApiException(code, status, message)` + Ktor StatusPages 글로벌 핸들러로 일괄 변환

### SDK
- **기본 동작**: silent fail + 내부 로그. 수집 실패가 호스트 앱을 크래시시키지 않는다.
- **선택적 에러 리스너**: 개발자가 원하면 에러 콜백을 등록할 수 있다.
- **HTTP 상태 분기**: 4xx(재시도 안 함), 5xx(지수 백오프 재시도), 409(중복 → 성공 처리)

## 10. API 설계 컨벤션

- **URL 경로**: kebab-case (`/admin/taxonomy-events`)
- **JSON 필드**: snake_case. Kotlin은 `JsonNamingStrategy.SnakeCase` 글로벌 설정으로 자동 변환한다. `@SerialName`을 일일이 쓰지 않는다.
- **페이지네이션**: 이벤트 조회(대량)는 cursor 기반, 관리자 목록(소량)은 offset 기반
- **날짜**: ISO-8601 UTC (`2026-10-22T10:00:00Z`). 서버 저장/응답은 UTC, 대시보드에서 타임존 변환
- **성공 응답**: `{ "data": ..., "meta": { ... } }`
- **에러 응답**: `{ "success": false, "error": { "code": "...", "message": "...", "details": {} } }`

## 11. 패키지/모듈 구조

### 패키지 네이밍
- 임시: `io.github.chaevi.analytics.*` (도메인 확정 후 변경)
- Backend: `io.github.chaevi.analytics.backend.*`
- SDK: `io.github.chaevi.analytics.sdk.*`

### Backend — 기능별 구조
```
backend/src/main/kotlin/io/github/chaevi/analytics/backend/
├── common/          # ApiException, Pagination, DatabaseFactory
├── organization/    # Route, Service, Repository, Dto
├── project/
├── application/
├── taxonomy/
├── ingest/
├── analytics/
└── Application.kt
```

### KMP SDK — 클린 아키텍처
```
sdk/analytics-kmp/src/
├── commonMain/kotlin/io/github/chaevi/analytics/sdk/
│   ├── domain/
│   │   ├── model/       # Event, Session, Identity 엔티티
│   │   ├── usecase/     # TrackEventUseCase, ManageSessionUseCase 등
│   │   └── repository/  # EventRepository, SessionRepository (인터페이스)
│   ├── data/
│   │   ├── repository/  # 인터페이스 구현체
│   │   ├── local/       # 로컬 저장 (expect 선언)
│   │   └── remote/      # HTTP 전송, 배치 API
│   └── api/
│       └── Analytics.kt # 외부 공개 API (Facade)
├── androidMain/         # actual 구현 (SharedPreferences, SQLDelight)
└── iosMain/             # actual 구현 (NSUserDefaults, SQLDelight)
```
- **domain**: 순수 비즈니스 로직, 외부 의존 없음, 단위 테스트 대상
- **data**: 네트워크/저장소 구현체, expect/actual 경계가 여기에 위치
- **public**: 호스트 앱이 사용하는 진입점 (Facade 패턴)

### Web Dashboard — 기능별 구조
```
web-dashboard/src/
├── features/        # organization, project, taxonomy, events, users, analytics
├── components/      # 공통 UI 컴포넌트
├── api/             # API 클라이언트
└── utils/
```

## 12. DB 규칙

- **테이블/컬럼**: snake_case + 복수형 (`organizations`, `analytics_events`)
- **마이그레이션**: Flyway, `V001__create_organizations.sql` 순번 방식
- **인덱스**: `idx_테이블_컬럼` (일반), `uq_테이블_컬럼` (유니크), `fk_테이블_컬럼` (외래키)

## 13. 보안 규칙

- **시크릿 관리**: 로컬은 `.env` 파일 (`.gitignore` 포함, `.env.example`만 커밋). 운영은 AWS Secrets Manager에서 주입.
- **입력 검증**: Route에서 형식 검증 (타입, 필수값, 길이, 포맷), Service에서 비즈니스 검증 (존재 여부, 권한, 중복)
- **CORS**: 명시적 허용 도메인만 등록. 와일드카드(`*`) 금지. 환경변수로 설정.
- **Ingestion key**: 공개 식별용, 관리자 권한 부여 안 함
- **관리자 인증**: 로컬은 간소화 가능, 운영 전 JWT 등 구현 필수
- **페이로드 제한**: 이벤트 배치 최대 크기 제한
- **Rate limiting**: 운영 시 수집 API에 적용
- **SQL Injection**: Exposed ORM 파라미터 바인딩으로 방지
- **XSS**: React 기본 이스케이프 + raw HTML 렌더링 금지
- **HTTPS**: 로컬은 HTTP 허용, 운영은 TLS 필수
