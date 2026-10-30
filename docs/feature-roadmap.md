# Feature Roadmap — 필수 기능 10가지

> 작성일: 2026-10-23

## 1. 필수 기능 목록

| 우선순위 | 기능 | 주요 내용 | 구현 상태 |
|----------|------|-----------|-----------|
| P0 | 프로젝트 및 앱 관리 | 여러 서비스 등록, Android/iOS/Web 구분 | 완료 |
| P0 | 이벤트 수집 | 클릭, 화면 이동, 사용자 행동 수집 | 완료 |
| P0 | 사용자 및 세션 관리 | 익명 ID, 로그인 ID, 세션 시작/종료 | 완료 |
| P0 | 화면 분석 | 화면 조회수, 방문자, 체류시간, 이탈률 | 완료 |
| P0 | 이벤트 분석 | 이벤트 발생 횟수, 사용자 수, 속성별 분석, 시계열 추이 | 완료 |
| P0 | SDK 관리 | API Key, 배치 전송, 오프라인 저장 | 완료 |
| P1 | 사용자 여정 분석 | 화면 및 이벤트 이동 경로 | 완료 |
| P1 | 퍼널 분석 | 단계별 전환율과 이탈률 | 완료 |
| P1 | 리텐션 분석 | 신규/재방문 사용자, D1/D7/D30 | 완료 |
| P1 | 텍소노미 관리 | 이벤트 및 속성 체계 정의/검증 | 부분 완료 |

## 2. 기능별 상세 현황

### P0 — 프로젝트 및 앱 관리 (완료)

- Organization / Project / Application CRUD API
- 앱별 플랫폼 구분 (ANDROID / IOS / WEB)
- 수집 키(Ingestion Key) 발급/폐기
- 대시보드에서 관리 UI 제공

### P0 — 이벤트 수집 (완료)

- `POST /ingest/events` 배치 수집 API
- 최대 100개 이벤트 배치 처리
- event_id 기반 idempotency (중복 방지)
- ARCHIVED 이벤트 수집 거부
- 미등록 이벤트 정책 (PERMISSIVE / STRICT)

### P0 — 사용자 및 세션 관리 (완료)

- 익명 ID (anonymous_id) 생성 및 영속화
- 로그인 ID (external_user_id) identify/resetIdentity
- 세션 타임아웃 (기본 30분) + 최대 지속 시간 (24시간)
- session_start / session_end 자동 전송 (KMP SDK)
- 서버에서 session_end 수신 시 ended_at 업데이트
- 대시보드에서 세션 상태 표시 (활성/종료/종료 추정)

### P0 — 화면 분석 (완료)

- 화면별 조회수 (view_count)
- 화면별 고유 사용자 수 (unique_users)
- 화면별 평균 체류시간 (`screen_leave` 이벤트 기반 집계)
- 화면별 이탈률 (`GET /analytics/bounce-rate`)
- 대시보드 화면 통계 탭 (체류시간 + 이탈률 컬럼 포함)

### P0 — 이벤트 분석 (완료)

- Overview: 총 이벤트 수, 세션 수, 사용자 수
- 이벤트 목록 조회 (날짜/이벤트키 필터, cursor 페이지네이션)
- 이벤트 상세 (properties, context JSON 전체)
- 이벤트별 발생 횟수 / 사용자 수 / 세션 수 집계 (`GET /events/summary`)
- 속성별 분석 (`GET /events/breakdown`)
- 시계열 추이 (`GET /analytics/timeseries`) — 일별/시간별, 기간 비교 지원
- DAU / WAU / MAU (`GET /analytics/active-users`)
- CSV 내보내기 (시계열, 이벤트 집계, 리텐션 등)
- 대시보드 시계열 차트 (recharts) + 기간 비교 오버레이

### P0 — SDK 관리 (완료)

- Ingestion Key 발급/폐기/상태 관리
- 배치 전송 (configurable batch size, flush interval)
- 지수 백오프 재시도 (1s → 2s → 4s, 최대 30s)
- 4xx 재시도 안 함, 5xx/네트워크 에러 시 재시도
- 플랫폼 context 수집 (KMP: os, device, locale 등)
- 인메모리 이벤트 큐 (최대 1000개)

### P1 — 사용자 여정 분석 (완료)

- 화면 간 전이 집계 (from_screen → to_screen + count)
- 대시보드 여정 탭에서 확인 가능

### P1 — 퍼널 분석 (완료)

- 다단계 퍼널 쿼리 (`POST /analytics/funnels/query`)
- 단계별 고유 사용자 수 + 전환율
- 전체 전환율 (overall_conversion_rate)

### P1 — 리텐션 분석 (완료)

구현 완료:
- `GET /analytics/retention` API (type=new/active, retention_days 설정 가능)
- type=new: 첫 이벤트 발생일 기준 신규 사용자 코호트
- type=active: 해당 날짜 활동 사용자 전체 코호트
- 코호트별 D-N 재방문 카운트 및 리텐션율 계산
- 대시보드 히트맵 테이블 (리텐션율별 색상 강도)
- 설계 문서: `docs/retention.md`

### P1 — 텍소노미 관리 (부분 완료)

구현 완료:
- 이벤트 등록/수정/목록/상세 CRUD
- 속성 추가/수정
- 이벤트 상태 관리 (ACTIVE / DEPRECATED / ARCHIVED)
- 시스템 이벤트 6개 자동 시딩
- 대시보드 텍소노미 관리 UI
- 텍소노미 설계서 작성 (채비 앱 전체 화면/이벤트/속성 정의)
- 설계서 기준 커스텀 이벤트 52개 + screen_leave = 총 59개 서버 등록
- 텍소노미 셋업 스크립트 (`scripts/setup-taxonomy.sh`)
- TrackScreenEvents 파라미터 제거 (screen_id 정규화)
- 플랫폼별 속성 값 정책 확정 (한 벌 관리, 값만 플랫폼별 허용)

미구현:
- 수집 시 필수 속성(required) 검증
- schema_version별 검증 규칙 적용
- 속성 타입 검증 (STRING/INTEGER/FLOAT 등)

### 세션 리플레이 (완료)

설계 문서: `docs/session-replay-design.md`

구현 완료:
- 세션 목록/타임라인 API + 대시보드 뷰어 (재생 0.5x~4x, 이벤트 패널)
- 앱 화면 React 컴포넌트 (플로우별 미리보기)
- **React 컴포넌트 방식 화면 렌더링** (9개 화면: splash, onboard, login, find_id, reset_password, register_term/pass/input/complete)
- **인터랙션 하이라이트** (Highlight 컴포넌트 + element_id 매칭 + 펄스 애니메이션)
- **화면 전환 애니메이션** (페이드아웃 + 슬라이드인)
- **이벤트 패널 동기화** (현재 화면 이벤트 하이라이트, 클릭 시 화면 이동/요소 강조)
- **플로우별 미리보기** (온보딩/로그인/회원가입 그룹핑 + 화살표 연결)
- Highlight 컴포넌트 (element_id 매칭 + 펄스 애니메이션)
- **SDK 채비 앱 적용** (trackScreen 8개 화면 + button_click 16개 요소)

설계 변경:
- SDK 자동 캡처 → **폐기** → JSON DSL 방식 → **React 컴포넌트 방식으로 전환**
- JSON 에디터/LayoutWireframe 렌더러 제거

### 대시보드 UX 통일 (완료)

- 공통 컴포넌트 3개 신규 생성 (`components/LoadingState.tsx`, `ErrorState.tsx`, `EmptyState.tsx`)
- `LoadingState`: 스피너 애니메이션 + 텍스트
- `ErrorState`: 에러 아이콘 + 메시지 + 재시도 버튼 (빨간 배경 카드)
- `EmptyState`: 아이콘 + 제목 + 설명
- 적용 페이지: EventsPage, EventSummaryPage, TrendPage, AnalyticsPage, RetentionPage, SessionListPage, SessionReplayPage, OverviewPage, TaxonomyPage (총 9개)
- RetentionPage, TaxonomyPage에 누락됐던 로딩/에러 상태 처리 추가

## 3. 다음 작업 우선순위 제안

| 순서 | 작업 | 사유 |
|------|------|------|
| 1 | ~~대시보드 UI 리팩토링~~ | → **완료** |
| 2 | ~~텍소노미 설계서 + 이벤트 등록~~ | → **완료** |
| 3 | ~~TrackScreenEvents 파라미터 제거~~ | → **완료** |
| 4 | ~~이벤트별 집계 API~~ | → **완료** |
| 5 | ~~화면 체류시간 계산~~ | → **완료** |
| 6 | ~~조직/프로젝트 관리 UX~~ | → **완료** |
| 7 | ~~parseDateRange 버그 수정~~ | → **완료** |
| 8 | ~~리텐션 분석 API + 대시보드~~ | → **완료** |
| 9 | ~~시계열 추이 API + 차트~~ | → **완료** |
| 10 | ~~세션 리플레이 ↔ 앱 화면 연동~~ | → **완료** |
| 11 | 텍소노미 필수 속성 검증 | 데이터 품질 보장 |
| 12 | 추가 화면 구현 (메인, 충전, 마이페이지) | 세션 리플레이 커버리지 확장 |
