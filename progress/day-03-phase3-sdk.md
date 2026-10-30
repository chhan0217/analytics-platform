# Day 03 — Phase 3: SDK

> 시작일: 2026-10-22

## 체크리스트

- [x] **Step 1.** KMP SDK Core — 이벤트 모델 + 설정
  - [x] domain/model: AnalyticsEvent, EventContext, IngestPayload, SdkInfo
  - [x] AnalyticsConfig 보강 (appVersion, sessionMaxDurationMs, maxRetries, enableLogging)

- [x] **Step 2.** KMP SDK Core — 익명 ID + 세션 관리
  - [x] domain/repository: StorageRepository (인터페이스)
  - [x] domain/usecase: IdentityManager (익명ID 생성/영속, identify, reset)
  - [x] domain/usecase: SessionManager (세션 생성/타임아웃/최대 지속시간, sequenceNumber)
  - [x] data/local: InMemoryStorage (기본 구현)

- [x] **Step 3.** KMP SDK Core — 이벤트 큐 + 배치 전송
  - [x] domain/repository: EventQueueRepository, EventSender (인터페이스)
  - [x] domain/usecase: EventDispatcher (배치 전송, 지수 백오프 재시도)
  - [x] data/local: InMemoryEventQueue
  - [x] data/remote: HttpEventSender (Ktor Client, POST /ingest/events)

- [x] **Step 4.** KMP SDK Core — Analytics Facade 구현
  - [x] api/Analytics.kt: initialize, track, trackScreen, identify, resetIdentity, flush
  - [x] 내부 코루틴 스코프 (SupervisorJob + Dispatchers.Default)
  - [x] 주기적 flush (flushIntervalMs 간격)
  - [x] 에러 리스너 콜백
  - [x] 클린 빌드 통과 (Android + iOS)

- [x] **Step 5.** Android 어댑터 (analytics-compose)
  - [x] TrackScreenEvents 실제 구현 (Navigation 화면 전환 → Analytics.trackScreen)
  - [x] AnalyticsButton (클릭 시 자동 button_click 이벤트 추적)
  - [x] 빌드 통과

- [x] **Step 6.** Android 샘플 앱 연동
  - [x] SDK 초기화 (10.0.2.2:8080, 에러 리스너)
  - [x] 버튼: 커스텀 이벤트, 화면 조회, identify/resetIdentity, flush
  - [x] AnalyticsButton 래퍼 사용
  - [x] APK 빌드 성공 (실제 전송 테스트는 Emulator에서)

- [x] **Step 7.** Web SDK 실제 구현
  - [x] 익명 ID (localStorage + crypto.randomUUID)
  - [x] 세션 관리 (비활동 타임아웃 + 최대 24시간)
  - [x] 이벤트 큐 + 배치 전송 (fetch API + 지수 백오프)
  - [x] identify / resetIdentity
  - [x] context 자동 수집 (브라우저, 화면, 로케일, 타임존)
  - [x] lint + 빌드 통과

- [x] **Step 8.** Web 샘플 앱 연동
  - [x] SDK 초기화 (localhost:8080 + Vite alias)
  - [x] 버튼: 커스텀 이벤트, 화면 조회, identify/reset, flush
  - [x] 빌드 성공

## 완료 조건
- KMP SDK: Android에서 이벤트 수집 → 로컬 서버 → DB 적재 동작
- Web SDK: 브라우저에서 이벤트 수집 → 로컬 서버 → DB 적재 동작
- 세션/익명 ID가 정상 생성·유지
- 재시도/큐 로직이 동작
- 단위 테스트 통과
- CI 통과

## 메모
(작업 중 결정사항, 이슈, 변경점 기록)
