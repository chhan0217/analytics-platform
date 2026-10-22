# Universal Analytics Platform

특정 서비스에 종속되지 않는 범용 고객 행동 분석 플랫폼.

## 사전 준비

| 도구 | 버전 | 설치 |
|---|---|---|
| JDK | 17+ | `brew install openjdk@17` |
| Node.js | 20+ | `brew install node` |
| Docker Desktop | 최신 | `brew install --cask docker` |
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
├── samples/
│   ├── android-app/          # Android 샘플 앱
│   └── web-app/              # Web 샘플 앱
├── infra/                    # Docker Compose
├── docs/                     # 설계 문서
├── scripts/                  # 로컬 실행 스크립트
└── progress/                 # 개발 진행 기록
```

## 로컬 실행

### 1. PostgreSQL 기동

```bash
./scripts/start-db.sh
```

또는 직접:

```bash
docker compose -f infra/docker-compose.yml up -d
```

### 2. Backend 서버 실행

```bash
./scripts/start-backend.sh
```

또는 직접:

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

브라우저에서 `http://localhost:5173` 접속.

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
# PostgreSQL 기동
docker compose -f infra/docker-compose.yml up -d

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

### Web Dashboard 테스트

```bash
cd web-dashboard && npm test
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
# PostgreSQL 종료
./scripts/stop-db.sh

# 또는 직접
docker compose -f infra/docker-compose.yml down
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

### Docker Desktop이 실행되지 않음

Docker Desktop 앱을 먼저 실행한 후 스크립트를 사용.

```bash
open /Applications/Docker.app
```

### Android Emulator에서 서버 접속 불가

Emulator에서 호스트 접근 주소는 `10.0.2.2`. Backend URL을 `http://10.0.2.2:8080`으로 설정.

## 알려진 제한사항

- **테스트 DB 의존**: Backend 테스트는 실제 PostgreSQL이 필요합니다. H2 인메모리 DB로 대체할 수 없습니다 (JSONB, PostgreSQL 전용 문법 사용).
- **관리자 인증**: 로컬 환경에서는 간소화된 JWT 인증을 사용합니다. 운영 배포 전 OAuth/OIDC 등 강화가 필요합니다.
- **Rate Limiting**: 수집 API에 Rate Limiting이 적용되어 있지 않습니다. 운영 시 별도 구성이 필요합니다.
- **SDK 세션 영속화**: iOS 플랫폼에서는 `NSUserDefaults` 기반으로 구현되어 있으며, 대용량 이벤트 큐에는 SQLDelight 마이그레이션이 권장됩니다.
- **Identity Linking**: `external_user_id` 기반 사용자 식별만 지원합니다. 디바이스 간 anonymous ID 병합은 미구현입니다.

## 문서

- [아키텍처](docs/architecture.md)
- [데이터 모델 (ERD)](docs/data-model.md)
- [이벤트 계약](docs/event-contract.md)
- [사용자 식별 및 세션](docs/identity-and-sessions.md)
- [텍소노미](docs/taxonomy.md)
- [API 설계](docs/api.md)
