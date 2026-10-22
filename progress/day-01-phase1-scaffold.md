# Day 01 — Phase 1: Scaffold & Contracts

> 시작일: 2026-10-22

## 체크리스트

- [x] **Step 1.** 설계 문서 작성
  - [x] docs/architecture.md (전체 아키텍처, 모듈 관계도)
  - [x] docs/event-contract.md (이벤트 스키마, 필수 필드, 배치 형식)
  - [x] docs/identity-and-sessions.md (익명ID, identify, 세션 정책)
  - [x] docs/taxonomy.md (텍소노미 구조, 미등록 이벤트 정책)
  - [x] docs/api.md (API 엔드포인트, 요청/응답, 에러 코드)
  - [x] docs/decisions/ 디렉토리 생성

- [x] **Step 2.** Git Flow 초기화
  - [x] develop 브랜치 생성
  - [x] 브랜치 보호 규칙 설정 (GitHub) — main, develop 모두 PR 필수

- [x] **Step 3.** Monorepo 루트 세팅
  - [x] settings.gradle.kts (멀티모듈)
  - [x] build.gradle.kts (루트)
  - [x] gradle/libs.versions.toml (Version Catalog)
  - [x] .gitignore
  - [x] .editorconfig

- [x] **Step 4.** 코드 품질 도구 세팅
  - [x] ktlint 설정 (subprojects 자동 적용)
  - [x] detekt 설정 (detekt/detekt.yml — 초기 느슨한 룰셋)
  - [x] husky + lint-staged 설정 (pre-commit 훅)

- [x] **Step 5.** 인프라 (Docker + DB)
  - [x] infra/docker-compose.yml (PostgreSQL 16 Alpine)
  - [x] infra/.env.example
  - [x] PostgreSQL 컨테이너 기동 확인

- [x] **Step 6.** Backend 스캐폴딩
  - [x] backend/build.gradle.kts
  - [x] Ktor Application.kt (서버 진입점)
  - [x] GET /health 엔드포인트
  - [x] DB 연결 확인

- [x] **Step 7.** KMP SDK 스캐폴딩
  - [x] sdk/analytics-kmp/build.gradle.kts (common, android, ios 타겟)
  - [x] commonMain 클린 아키텍처 패키지 구조
  - [x] Analytics.kt 빈 Facade
  - [x] 빌드 확인 (Android + iOS 모두 성공)

- [x] **Step 8.** Compose 어댑터 스캐폴딩
  - [x] sdk/analytics-compose/build.gradle.kts
  - [x] AnalyticsNavigation.kt (화면 전환 추적 껍데기)
  - [x] Compose 함수명 ktlint 예외 설정
  - [x] 빌드 확인

- [x] **Step 9.** Web SDK 스캐폴딩
  - [x] sdk/analytics-web/package.json
  - [x] tsconfig.json, ESLint (flat config), Prettier 설정
  - [x] analytics.ts (빈 Facade) + types.ts + index.ts (진입점)
  - [x] tsup 번들러 설정 (CJS + ESM + d.ts)
  - [x] lint + format + 빌드 확인

- [x] **Step 10.** Web Dashboard 스캐폴딩
  - [x] web-dashboard/package.json (Vite + React + TypeScript)
  - [x] ESLint (flat config) + Prettier 설정
  - [x] 기본 홈 화면
  - [x] lint + format + 빌드 확인

- [x] **Step 11.** Android 샘플 앱
  - [x] samples/android-app/build.gradle.kts (KMP + Compose + SDK 의존)
  - [x] MainActivity.kt (Compose 기본 화면)
  - [x] AndroidManifest, 리소스 (strings, themes)
  - [x] APK 빌드 확인

- [x] **Step 12.** Web 샘플 앱
  - [x] samples/web-app/package.json (Vite + TypeScript)
  - [x] index.html + main.ts (기본 페이지)
  - [x] 빌드 확인

- [x] **Step 13.** 로컬 실행 스크립트
  - [x] scripts/start-db.sh (Docker PostgreSQL 기동)
  - [x] scripts/stop-db.sh (Docker PostgreSQL 종료)
  - [x] scripts/start-backend.sh (Ktor 서버 실행)
  - [x] scripts/start-web.sh (Vite 대시보드 실행)

- [x] **Step 14.** README
  - [x] 사전 준비 (JDK, Node, Docker 등)
  - [x] 실행 명령 및 포트 안내
  - [x] 빌드 / 코드 품질 명령
  - [x] 트러블슈팅 (PostgreSQL 충돌, 메모리, Docker, Emulator)

- [x] **Step 15.** CI (GitHub Actions)
  - [x] .github/workflows/ci-backend.yml
  - [x] .github/workflows/ci-sdk-kmp.yml
  - [x] .github/workflows/ci-web-dashboard.yml
  - [x] .github/workflows/ci-sdk-web.yml
  - [x] 전체 CI 통과 확인 (PR #1에서 4개 모두 통과)

## 완료 조건
- 모든 모듈이 독립적으로 빌드 성공
- Docker PostgreSQL 기동 + Backend 연결 확인
- CI 전체 통과
- README 따라 했을 때 로컬에서 실행 가능

## 메모
(작업 중 결정사항, 이슈, 변경점 기록)
