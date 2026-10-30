# Universal Analytics Platform

특정 서비스에 종속되지 않는 범용 고객 행동 분석 플랫폼.

## 주요 기능

| 기능 | 설명 |
|------|------|
| **이벤트 수집** | 배치 API (최대 100건), 중복 방지, PERMISSIVE/STRICT 정책 |
| **사용자/세션 관리** | 익명 ID + 로그인 ID 식별, 세션 타임아웃 30분, 자동 session_start/end |
| **화면 분석** | 조회수, 고유 사용자, 평균 체류시간, 이탈률 + 히트맵 색상 |
| **이벤트 분석** | 이벤트 집계, 속성별 분석, 시계열 추이, 기간 비교, DAU/WAU/MAU |
| **퍼널 분석** | 다단계 전환율/이탈률 분석 |
| **여정 분석** | 화면 간 전이 경로 집계 |
| **리텐션 분석** | new/active 코호트 기반 D1/D7/D30 재방문율 히트맵 |
| **세션 리플레이** | React 컴포넌트 기반 화면 재생 + 인터랙션 하이라이트 |
| **텍소노미 관리** | 이벤트/속성 체계 정의, 상태 관리, 시스템 이벤트 시딩 |
| **CSV 내보내기** | 시계열, 이벤트 집계, 리텐션 등 데이터 다운로드 |

## 사전 준비

| 도구 | 버전 | 설치 |
|---|---|---|
| JDK | 17+ | `brew install openjdk@17` |
| Node.js | 20+ | `brew install node` |
| PostgreSQL | 17 | `brew install postgresql@17` |
| Android Studio | 최신 | [developer.android.com](https://developer.android.com/studio) |

## 프로젝트 구조

```
analytics-platform/
├── backend/                  # Ktor 서버 (Kotlin)
├── sdk/
│   ├── analytics-kmp/        # KMP SDK (Android/iOS)
│   ├── analytics-compose/    # Jetpack Compose 어댑터
│   └── analytics-web/        # Web SDK (TypeScript)
├── web-dashboard/            # React 관리자 대시보드
│   └── src/features/
│       ├── overview/         # 개요 (KPI + 증감률)
│       ├── events/           # 이벤트 목록 + 집계
│       ├── analytics/        # 화면분석 + 퍼널 + 여정 + 리텐션 + 시계열
│       ├── replay/           # 세션 리플레이
│       ├── screens/          # 앱 화면 미리보기 + React 화면 컴포넌트
│       ├── users/            # 사용자 검색
│       └── taxonomy/         # 텍소노미 관리
├── docs/                     # 설계 문서
└── scripts/                  # 로컬 실행 스크립트
```

## 로컬 실행

### 1. PostgreSQL 기동

```bash
brew services start postgresql@17
```

### 2. Backend 서버 실행

```bash
./gradlew :backend:run
```

서버 확인:

```bash
curl http://localhost:8080/health
```

### 3. Web Dashboard 실행

```bash
cd web-dashboard
npm install    # 최초 1회
npm run dev
```

브라우저에서 `http://localhost:5173` 접속. 기본 관리자 계정: `admin@analytics.local` / `admin1234`

### 4. Android 샘플 앱

Android Studio에서 프로젝트 열기 → `samples:android-app` 실행.

Emulator에서 Backend 접속 주소: `http://10.0.2.2:8080`

### 5. Web 샘플 앱

```bash
cd samples/web-app
npm install    # 최초 1회
npm run dev
```

## 포트 안내

| 구성 요소 | 주소 |
|---|---|
| PostgreSQL | `localhost:5432` |
| Backend (Ktor) | `localhost:8080` |
| Web Dashboard (Vite) | `localhost:5173` |
| Web Sample (Vite) | `localhost:5174` |

## DB 접속 정보

| 항목 | 값 |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `analytics` |
| User | `analytics` |
| Password | `analytics_local` |

## 빌드

```bash
# 전체 Kotlin 모듈 빌드
./gradlew build

# 개별 모듈
./gradlew :backend:build
./gradlew :sdk:analytics-kmp:build
./gradlew :sdk:analytics-compose:build
./gradlew :samples:android-app:assembleDebug

# Web SDK
cd sdk/analytics-web && npm run build

# Web Dashboard
cd web-dashboard && npm run build
```

## 테스트

### Backend 테스트

PostgreSQL이 실행 중이어야 합니다.

```bash
# 전체 테스트 (단위 + 통합 + E2E)
./gradlew :backend:test

# E2E 테스트만
./gradlew :backend:test --tests "io.github.chaevi.analytics.backend.e2e.*"
```

### E2E 테스트 시나리오

| 테스트 클래스 | 시나리오 | 검증 내용 |
|---|---|---|
| `ProjectIsolationTest` | 멀티 프로젝트 격리 | 동일 `external_user_id`가 프로젝트 간 격리 |
| `ProjectIsolationTest` | 크로스 플랫폼 이벤트 통합 | Android/Web 이벤트가 같은 프로젝트에서 통합 조회 |
| `ProjectIsolationTest` | 크로스 플랫폼 세션 통합 | 동일 사용자의 Android/Web 세션이 모두 조회 |
| `IngestValidationTest` | Idempotency | 동일 `event_id` 재전송 시 중복 저장 방지 |
| `IngestValidationTest` | ARCHIVED 이벤트 거부 | 보관된 이벤트 수집 시 400 응답 |
| `IngestValidationTest` | 잘못된 수집 키 | 유효하지 않은 키로 요청 시 401 응답 |
| `IngestValidationTest` | 배치 크기 검증 | 빈 배치 400, 100개 초과 413 응답 |
| `AnalyticsAccuracyTest` | Overview 수치 | 이벤트/세션/사용자 수 정확성 |
| `AnalyticsAccuracyTest` | Screen 통계 | 화면별 조회수/고유 사용자 수 정확성 |
| `AnalyticsAccuracyTest` | Funnel 전환율 | 단계별 전환 사용자 수 및 전환율 정확성 |

### Web SDK 테스트

```bash
cd sdk/analytics-web && npm test
```

### KMP SDK 테스트

```bash
./gradlew :sdk:analytics-kmp:allTests
```

## 코드 품질

```bash
# Kotlin 포맷팅 검사
./gradlew ktlintCheck

# Kotlin 자동 수정
./gradlew ktlintFormat

# Kotlin 정적 분석
./gradlew detekt

# Web Dashboard 린트
cd web-dashboard && npm run lint

# Web SDK 린트
cd sdk/analytics-web && npm run lint
```

## 종료

```bash
brew services stop postgresql@17
```

## 트러블슈팅

### PostgreSQL 연결 실패 (`role "analytics" does not exist`)

로컬에 설치된 PostgreSQL이 5432 포트를 점유하고 있을 수 있음.

```bash
# 로컬 PostgreSQL 확인
lsof -i :5432

# brew로 설치된 경우 중지
brew services stop postgresql@17
```

### Gradle 메모리 부족 (iOS 빌드 시)

`gradle.properties`에서 메모리 설정 확인:

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

### Android Emulator에서 서버 접속 불가

Emulator에서 호스트 접근 주소는 `10.0.2.2`. Backend URL을 `http://10.0.2.2:8080`으로 설정.

## 알려진 제한사항

- **테스트 DB 의존**: Backend 테스트는 실제 PostgreSQL이 필요합니다. H2 인메모리 DB로 대체할 수 없습니다 (JSONB, PostgreSQL 전용 문법 사용).
- **관리자 인증**: 로컬 환경에서는 간소화된 JWT 인증을 사용합니다. 운영 배포 전 OAuth/OIDC 등 강화가 필요합니다.
- **Rate Limiting**: 수집 API에 Rate Limiting이 적용되어 있지 않습니다. 운영 시 별도 구성이 필요합니다.
- **텍소노미 검증**: 수집 시 필수 속성 검증, schema_version별 검증은 미구현입니다.
- **SDK context (Web)**: `browser_version`/`os_version` 파싱이 미구현입니다.
- **Identity Linking**: 디바이스 간 anonymous ID 병합은 미구현입니다.

## 문서

- [아키텍처](docs/architecture.md)
- [데이터 모델 (ERD)](docs/data-model.md)
- [이벤트 계약](docs/event-contract.md)
- [사용자 식별 및 세션](docs/identity-and-sessions.md)
- [텍소노미](docs/taxonomy.md)
- [API 설계](docs/api.md)
- [세션 리플레이 설계](docs/session-replay-design.md)
- [리텐션 분석](docs/retention.md)
- [시계열 추이](docs/trend.md)
- [SDK 연동 가이드 (Android)](docs/sdk-integration-guide-android.md)
- [기능 로드맵](docs/feature-roadmap.md)
- [UI/UX 개선 계획](docs/ui-improvement-plan.md)
