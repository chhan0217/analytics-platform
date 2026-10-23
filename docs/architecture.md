# Architecture — Universal Analytics Platform

## 1. 개요

특정 서비스에 종속되지 않는 범용 고객 행동 분석 플랫폼이다.
Organization → Project → Application 계층으로 멀티 테넌트를 지원하며,
Android/iOS/Web 클라이언트에서 발생한 이벤트를 수집·저장·분석한다.

## 2. 시스템 구성도

```
┌─────────────────────────────────────────────────────────────┐
│                        Clients                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Android App  │  │   iOS App    │  │   Web App    │      │
│  │ (KMP SDK)    │  │ (KMP SDK)    │  │ (Web SDK)    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │ POST /ingest     │                  │
          │ /events          │                  │
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                     Ktor Backend                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Ingest API  │  │  Admin API   │  │ Analytics API│      │
│  │  (수집)      │  │  (관리)      │  │  (분석)      │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         └─────────────────┼─────────────────┘               │
│                           ▼                                 │
│              ┌────────────────────┐                         │
│              │   Service Layer    │                         │
│              └─────────┬──────────┘                         │
│                        ▼                                    │
│              ┌────────────────────┐                         │
│              │  Repository Layer  │                         │
│              └─────────┬──────────┘                         │
└────────────────────────┼────────────────────────────────────┘
                         ▼
              ┌────────────────────┐
              │    PostgreSQL      │
              │  (Docker Compose)  │
              └────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   Web Dashboard                             │
│  ┌──────────────────────────────────────────────────┐      │
│  │  React + TypeScript + Vite                       │      │
│  │  (Admin API / Analytics API 호출)                │      │
│  └──────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

## 3. 모듈 구성

```
analytics-platform/
├── backend/                  # Ktor 서버 (수집, 관리, 분석 API)
├── sdk/
│   ├── analytics-kmp/        # KMP SDK (commonMain, androidMain, iosMain)
│   ├── analytics-compose/    # Jetpack Compose 어댑터
│   └── analytics-web/        # TypeScript Web SDK
├── web-dashboard/            # React 관리자 대시보드
├── samples/
│   ├── android-app/          # Android 샘플 앱
│   └── web-app/              # Web 샘플 앱
├── infra/                    # Docker Compose, 환경 설정
├── docs/                     # 설계 문서
├── scripts/                  # 로컬 실행 스크립트
└── progress/                 # 개발 진행 기록
```

### 모듈 간 의존 관계

```
samples/android-app → sdk/analytics-kmp, sdk/analytics-compose
samples/web-app     → sdk/analytics-web
sdk/analytics-compose → sdk/analytics-kmp
web-dashboard       → backend (HTTP API)
sdk/analytics-kmp   → backend (HTTP API, 수집 전용)
sdk/analytics-web   → backend (HTTP API, 수집 전용)
backend             → PostgreSQL
```

- 모듈 간 코드 의존은 SDK 내부에서만 발생한다 (analytics-compose → analytics-kmp).
- Backend, Web Dashboard, SDK는 HTTP API로만 통신한다.
- 공유되는 것은 이벤트 계약 문서(docs/event-contract.md)이지 코드가 아니다.

## 4. 테넌트 계층

```
Organization (조직)
└── Project (서비스)
    └── Application (플랫폼별 클라이언트)
        ├── platform: ANDROID | IOS | WEB
        ├── app_identifier: 패키지명 또는 도메인
        └── app_credentials: ingestion key
```

- 하나의 Organization은 여러 Project를 가진다.
- 하나의 Project는 여러 Application을 가진다 (같은 플랫폼도 복수 가능).
- Application마다 ingestion key가 발급되며, 이 키로 이벤트의 소속을 판별한다.
- 사용자 식별은 Project 범위로 격리된다.

## 5. 기술 스택

| 구성 요소 | 기술 |
|---|---|
| Backend | Kotlin, Ktor, Gradle Kotlin DSL, kotlinx.serialization, Exposed, Flyway |
| Database | PostgreSQL (JSONB, index), Docker Compose |
| Web Dashboard | React, TypeScript, Vite, React Router, TanStack Query |
| KMP SDK | Kotlin Multiplatform, Ktor Client, kotlinx.coroutines, kotlinx.serialization, SQLDelight |
| Android 어댑터 | Jetpack Compose + Navigation 연동 |
| Web SDK | TypeScript |
| CI | GitHub Actions |

## 6. 패키지 네이밍

- Backend: `io.github.chaevi.analytics.backend.*`
- SDK: `io.github.chaevi.analytics.sdk.*`
- 도메인 확정 후 변경 예정 (임시로 GitHub 기반 사용)

## 7. 로컬 실행 구성

| 구성 요소 | 실행 방식 | 주소 |
|---|---|---|
| PostgreSQL | Docker Compose | `localhost:5432` |
| Ktor Backend | 로컬 JVM | `localhost:8080` |
| React Dashboard | Vite 개발 서버 | `localhost:5173` |
| Android 샘플 | Android Studio + Emulator | Emulator → `10.0.2.2:8080` |
| Web 샘플 | 로컬 브라우저 | `localhost:5174` (또는 별도 포트) |

## 8. 대시보드 UI 구조

```
┌──────────────────────────────────────────────────────────┐
│  헤더: [조직 선택 ▼] / [프로젝트 선택 ▼]         [로그아웃] │
├──────────┬───────────────────────────────────────────────┤
│ 분석     │  메인 콘텐츠                                   │
│  개요    │  (선택한 메뉴에 따라 표시)                      │
│  이벤트  │                                               │
│  화면분석│                                               │
│  퍼널    │                                               │
│  여정    │                                               │
│  사용자  │                                               │
│ 설정     │                                               │
│  앱 관리 │                                               │
│  텍소노미│                                               │
└──────────┴───────────────────────────────────────────────┘
```

- 프로젝트 선택은 헤더 드롭다운으로 수행 (Firebase/Amplitude 표준 패턴)
- 선택 상태는 `ProjectContext` + `localStorage`에 영속화
- 사이드바는 분석 메뉴와 설정 메뉴로 분류
- 개요 페이지에서 핵심 지표(이벤트/세션/사용자 수) + 화면별 통계를 한눈에 확인

## 9. 운영 배포 구성 (Phase 6 이후)

| 구성 요소 | 배포 대상 |
|---|---|
| Backend | AWS ECS Fargate |
| PostgreSQL | AWS RDS PostgreSQL |
| Web Dashboard | AWS S3 + CloudFront |
| KMP SDK | Maven Central |
| Web SDK | npm |
