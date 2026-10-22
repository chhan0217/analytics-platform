# Data Model — 데이터베이스 설계

## 1. ERD (Entity Relationship Diagram)

```
┌──────────────────┐
│  organizations   │
│──────────────────│
│ id          (PK) │
│ name             │
│ created_at       │
└────────┬─────────┘
         │ 1:N
         ▼
┌──────────────────┐       ┌──────────────────────┐
│    projects      │       │    admin_users        │
│──────────────────│       │──────────────────────│
│ id          (PK) │       │ id          (PK)     │
│ organization_id  │──┐    │ email                │
│ name             │  │    │ password_hash        │
│ unregistered_    │  │    │ name                 │
│   event_policy   │  │    │ created_at           │
│ created_at       │  │    └──────────┬───────────┘
└──┬───┬───┬───────┘  │               │
   │   │   │          │               │ N:M
   │   │   │          │    ┌──────────▼───────────┐
   │   │   │          │    │ project_memberships   │
   │   │   │          │    │──────────────────────│
   │   │   │          └────│ project_id      (FK) │
   │   │   │               │ admin_user_id   (FK) │
   │   │   │               │ role                 │
   │   │   │               └──────────────────────┘
   │   │   │
   │   │   │ 1:N
   │   │   ▼
   │   │  ┌──────────────────┐     ┌──────────────────────┐
   │   │  │  applications    │     │  admin_audit_logs     │
   │   │  │──────────────────│     │──────────────────────│
   │   │  │ id          (PK) │     │ id            (PK)   │
   │   │  │ project_id  (FK) │     │ admin_user_id (FK)   │
   │   │  │ name             │     │ action               │
   │   │  │ platform         │     │ target_type          │
   │   │  │ app_identifier   │     │ target_id            │
   │   │  │ created_at       │     │ details     (JSONB)  │
   │   │  └────────┬─────────┘     │ occurred_at          │
   │   │           │ 1:N           └──────────────────────┘
   │   │           ▼
   │   │  ┌──────────────────┐
   │   │  │ app_credentials  │
   │   │  │──────────────────│
   │   │  │ id          (PK) │
   │   │  │ application_id   │
   │   │  │ key_prefix       │
   │   │  │ key_hash   (UQ)  │
   │   │  │ status           │
   │   │  │ created_at       │
   │   │  │ revoked_at       │
   │   │  └──────────────────┘
   │   │
   │   │ 1:N
   │   ▼
   │  ┌─────────────────────┐     ┌──────────────────────┐
   │  │  taxonomy_events    │     │ taxonomy_properties   │
   │  │─────────────────────│     │──────────────────────│
   │  │ id          (PK)    │──┐  │ id            (PK)   │
   │  │ project_id  (FK)    │  │  │ taxonomy_event_id    │
   │  │ event_key   (UQ*)   │  └─▶│ property_key  (UQ*)  │
   │  │ display_name        │     │ value_type           │
   │  │ description         │     │ required             │
   │  │ schema_version      │     │ description          │
   │  │ status              │     └──────────────────────┘
   │  │ is_system           │
   │  │ created_at          │
   │  └─────────────────────┘
   │
   │ 1:N (이벤트/세션/사용자)
   ▼
┌─────────────────────────┐
│    analytics_events     │
│─────────────────────────│
│ id (PK, 클라이언트 UUID)│
│ project_id        (FK)  │
│ application_id    (FK)  │
│ session_id              │
│ analytics_user_id (FK)  │
│ anonymous_id            │
│ event_key               │
│ event_schema_version    │
│ properties     (JSONB)  │
│ context        (JSONB)  │
│ occurred_at             │
│ received_at             │
│ sequence_number         │
└─────────────────────────┘

┌─────────────────────────┐     ┌──────────────────────┐
│   analytics_sessions    │     │   analytics_users     │
│─────────────────────────│     │──────────────────────│
│ id            (PK)      │     │ id            (PK)   │
│ project_id    (FK)      │     │ project_id    (FK)   │
│ application_id(FK)      │     │ external_user_id     │
│ anonymous_id            │     │ created_at           │
│ analytics_user_id (FK)  │     │ updated_at           │
│ started_at              │     └──────────┬───────────┘
│ last_event_at           │                │
│ ended_at                │                │ 1:N
└─────────────────────────┘     ┌──────────▼───────────┐
                                │   identity_links      │
┌─────────────────────────┐     │──────────────────────│
│  anonymous_identities   │     │ id            (PK)   │
│─────────────────────────│     │ project_id    (FK)   │
│ id            (PK)      │────▶│ analytics_user_id    │
│ project_id    (FK)      │     │ anonymous_identity_id│
│ anonymous_id  (UQ*)     │     │ linked_at            │
│ created_at              │     │ unlinked_at          │
└─────────────────────────┘     └──────────────────────┘

(UQ*) = 복합 유니크 제약
```

## 2. 테넌트 계층

```
Organization (조직)
  └── Project (서비스)
        ├── Application (플랫폼별 앱)
        │     └── AppCredential (수집 키)
        ├── TaxonomyEvent (이벤트 정의)
        │     └── TaxonomyProperty (속성 정의)
        ├── AnalyticsUser (사용자)
        ├── AnonymousIdentity (익명 ID)
        ├── IdentityLink (익명↔로그인 연결)
        ├── AnalyticsSession (세션)
        └── AnalyticsEvent (이벤트)
```

## 3. 테이블 상세

### 3.1 organizations (조직)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK, DEFAULT gen_random_uuid() | 조직 ID |
| name | VARCHAR(256) | NOT NULL | 조직명 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | 생성 시각 |

### 3.2 projects (프로젝트/서비스)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 프로젝트 ID |
| organization_id | UUID | FK → organizations | 소속 조직 |
| name | VARCHAR(256) | NOT NULL | 프로젝트명 |
| unregistered_event_policy | VARCHAR(20) | NOT NULL, DEFAULT 'PERMISSIVE' | 미등록 이벤트 정책 (PERMISSIVE/STRICT/QUARANTINE) |
| created_at | TIMESTAMP | NOT NULL | 생성 시각 |

### 3.3 applications (앱)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 앱 ID |
| project_id | UUID | FK → projects | 소속 프로젝트 |
| name | VARCHAR(256) | NOT NULL | 앱 이름 |
| platform | VARCHAR(20) | NOT NULL, CHECK (ANDROID/IOS/WEB) | 플랫폼 |
| app_identifier | VARCHAR(256) | NULL | 패키지명 또는 도메인 |
| created_at | TIMESTAMP | NOT NULL | 생성 시각 |

### 3.4 app_credentials (수집 키)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 자격 증명 ID |
| application_id | UUID | FK → applications | 소속 앱 |
| key_prefix | VARCHAR(12) | NOT NULL | 키 앞 8자 (관리자 식별용) |
| key_hash | VARCHAR(64) | NOT NULL, UNIQUE | SHA-256 해시 |
| status | VARCHAR(20) | NOT NULL, CHECK (ACTIVE/REVOKED) | 상태 |
| created_at | TIMESTAMP | NOT NULL | 생성 시각 |
| revoked_at | TIMESTAMP | NULL | 폐기 시각 |

**수집 키 흐름**: 발급 시 `ink_` + 32자 랜덤 hex 생성 → 원본 1회 반환 → DB에는 SHA-256 해시만 저장 → 수집 API에서 키 해시로 조회

### 3.5 admin_users (관리자)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 관리자 ID |
| email | VARCHAR(256) | NOT NULL, UNIQUE | 이메일 |
| password_hash | VARCHAR(256) | NOT NULL | 비밀번호 해시 |
| name | VARCHAR(128) | NOT NULL | 이름 |
| created_at | TIMESTAMP | NOT NULL | 생성 시각 |

### 3.6 project_memberships (프로젝트 멤버십)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 멤버십 ID |
| admin_user_id | UUID | FK → admin_users | 관리자 |
| project_id | UUID | FK → projects | 프로젝트 |
| role | VARCHAR(20) | CHECK (OWNER/ADMIN/MEMBER) | 역할 |
| created_at | TIMESTAMP | NOT NULL | 생성 시각 |

**UNIQUE**: (admin_user_id, project_id) — 한 관리자가 한 프로젝트에 하나의 역할

### 3.7 taxonomy_events (텍소노미 이벤트)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 텍소노미 이벤트 ID |
| project_id | UUID | FK → projects | 소속 프로젝트 |
| event_key | VARCHAR(128) | NOT NULL | 이벤트 키 (snake_case) |
| display_name | VARCHAR(256) | NOT NULL | 표시명 |
| description | TEXT | NULL | 설명 |
| schema_version | INTEGER | NOT NULL, DEFAULT 1 | 스키마 버전 |
| status | VARCHAR(20) | CHECK (ACTIVE/DEPRECATED/ARCHIVED) | 상태 |
| is_system | BOOLEAN | NOT NULL, DEFAULT false | 시스템 이벤트 여부 |
| created_at | TIMESTAMP | NOT NULL | 생성 시각 |

**UNIQUE**: (project_id, event_key) — 프로젝트 내 이벤트 키 유일

### 3.8 taxonomy_properties (텍소노미 속성)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 속성 ID |
| taxonomy_event_id | UUID | FK → taxonomy_events (CASCADE) | 소속 이벤트 |
| property_key | VARCHAR(128) | NOT NULL | 속성 키 |
| value_type | VARCHAR(20) | CHECK (STRING/INTEGER/FLOAT/BOOLEAN/ARRAY/OBJECT) | 값 타입 |
| required | BOOLEAN | NOT NULL, DEFAULT false | 필수 여부 |
| description | TEXT | NULL | 설명 |

**UNIQUE**: (taxonomy_event_id, property_key) — 이벤트 내 속성 키 유일

### 3.9 analytics_users (분석 사용자)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 내부 사용자 ID |
| project_id | UUID | FK → projects | 소속 프로젝트 |
| external_user_id | VARCHAR(256) | NULL | 로그인 사용자 ID (nullable) |
| created_at | TIMESTAMP | NOT NULL | 생성 시각 |
| updated_at | TIMESTAMP | NOT NULL | 수정 시각 |

**UNIQUE (partial)**: (project_id, external_user_id) WHERE external_user_id IS NOT NULL

### 3.10 anonymous_identities (익명 ID)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 내부 ID |
| project_id | UUID | FK → projects | 소속 프로젝트 |
| anonymous_id | UUID | NOT NULL | SDK가 생성한 익명 ID |
| created_at | TIMESTAMP | NOT NULL | 생성 시각 |

**UNIQUE**: (project_id, anonymous_id)

### 3.11 identity_links (식별 연결)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 연결 ID |
| project_id | UUID | FK → projects | 소속 프로젝트 |
| analytics_user_id | UUID | FK → analytics_users | 로그인 사용자 |
| anonymous_identity_id | UUID | FK → anonymous_identities | 익명 ID |
| linked_at | TIMESTAMP | NOT NULL | 연결 시각 |
| unlinked_at | TIMESTAMP | NULL | 해제 시각 |

### 3.12 analytics_sessions (세션)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 세션 ID |
| project_id | UUID | FK → projects | 소속 프로젝트 |
| application_id | UUID | FK → applications | 소속 앱 |
| anonymous_id | UUID | NOT NULL | 익명 ID |
| analytics_user_id | UUID | FK → analytics_users, NULL | 로그인 사용자 (있으면) |
| started_at | TIMESTAMP | NOT NULL | 세션 시작 시각 |
| last_event_at | TIMESTAMP | NOT NULL | 마지막 이벤트 시각 |
| ended_at | TIMESTAMP | NULL | 세션 종료 시각 |

### 3.13 analytics_events (이벤트)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | **PK (클라이언트 event_id)** | 이벤트 ID — idempotency 보장 |
| project_id | UUID | FK → projects | 소속 프로젝트 (서버 결정) |
| application_id | UUID | FK → applications | 소속 앱 (서버 결정) |
| session_id | UUID | NOT NULL | 세션 ID |
| analytics_user_id | UUID | FK → analytics_users, NULL | 로그인 사용자 |
| anonymous_id | UUID | NOT NULL | 익명 ID |
| event_key | VARCHAR(128) | NOT NULL | 이벤트 종류 |
| event_schema_version | INTEGER | NOT NULL | 스키마 버전 |
| properties | JSONB | NOT NULL, DEFAULT '{}' | 이벤트 속성 |
| context | JSONB | NOT NULL, DEFAULT '{}' | 디바이스/앱 환경 |
| occurred_at | TIMESTAMP | NOT NULL | 발생 시각 (클라이언트) |
| received_at | TIMESTAMP | NOT NULL, DEFAULT now() | 수신 시각 (서버) |
| sequence_number | INTEGER | NULL | 세션 내 순서 |

**인덱스**:
- `(project_id, occurred_at)` — 프로젝트별 시간순 조회
- `(application_id, occurred_at)` — 앱별 시간순 조회
- `(project_id, session_id, occurred_at)` — 세션 내 이벤트 조회
- `(project_id, analytics_user_id, occurred_at)` — 사용자별 이벤트 조회
- `(project_id, event_key)` — 이벤트 종류별 조회

### 3.14 admin_audit_logs (감사 로그)

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | UUID | PK | 로그 ID |
| admin_user_id | UUID | FK → admin_users | 수행한 관리자 |
| action | VARCHAR(64) | NOT NULL | 수행 액션 (CREATE, UPDATE, DELETE 등) |
| target_type | VARCHAR(64) | NOT NULL | 대상 엔티티 (ORGANIZATION, PROJECT 등) |
| target_id | UUID | NULL | 대상 ID |
| details | JSONB | NULL | 변경 상세 |
| occurred_at | TIMESTAMP | NOT NULL | 발생 시각 |

## 4. 설계 결정 사항

### analytics_events.id = 클라이언트 event_id
- 클라이언트가 생성한 UUID를 PK로 직접 사용
- 동일 event_id 재전송 시 PK 충돌로 자연스럽게 idempotency 보장
- 별도 중복 체크 로직 불필요

### Partial Unique Index (analytics_users)
- `(project_id, external_user_id) WHERE external_user_id IS NOT NULL`
- 익명 사용자는 external_user_id가 NULL이므로 유일성 제약에서 제외
- 로그인 사용자만 프로젝트 내 유일성 보장

### 수집 키 해시 저장
- 원본 키는 발급 시 1회만 반환, DB에는 SHA-256 해시만 저장
- key_prefix (앞 8자)는 관리자가 키를 식별하기 위한 참조용
- DB 유출 시에도 원본 키 복원 불가

### JSONB 활용
- analytics_events.properties — 이벤트별 커스텀 속성, 스키마가 유동적
- analytics_events.context — 디바이스/환경 정보
- admin_audit_logs.details — 변경 상세
- PostgreSQL JSONB 인덱스로 향후 속성 기반 쿼리 가능

### 시간 처리
- 모든 TIMESTAMP는 UTC 기준
- occurred_at: 클라이언트 시계 (부정확할 수 있음)
- received_at: 서버 시계 (정렬 보조 기준)
- created_at: 레코드 생성 시각
