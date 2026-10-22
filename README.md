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

## 문서

- [아키텍처](docs/architecture.md)
- [데이터 모델 (ERD)](docs/data-model.md)
- [이벤트 계약](docs/event-contract.md)
- [사용자 식별 및 세션](docs/identity-and-sessions.md)
- [텍소노미](docs/taxonomy.md)
- [API 설계](docs/api.md)
