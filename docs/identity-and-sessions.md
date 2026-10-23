# Identity & Sessions — 사용자 식별 및 세션 정책

## 1. 개요

사용자 식별은 익명 ID와 로그인 ID의 이중 체계로 운영한다.
세션은 활동 기반으로 관리하며, 비활동 타임아웃으로 자동 종료한다.
모든 식별은 Project 범위로 격리된다.

## 2. 식별 체계

### 2.1 익명 ID (Anonymous ID)

- SDK 초기화 시 UUID v4로 생성
- 설치/브라우저 인스턴스 범위로 영속화
  - Android: SharedPreferences 또는 SQLDelight
  - iOS: NSUserDefaults 또는 SQLDelight
  - Web: localStorage
- 삭제/재설치/브라우저 저장소 초기화 시 새 ID 생성
- 앱 업데이트 시에는 유지

### 2.2 로그인 사용자 ID (External User ID)

- `Analytics.identify(externalUserId)` 호출로 설정
- 호스트 앱의 로그인 계정 ID (서비스 내부 ID 권장)
- 설정 이후 모든 이벤트에 `external_user_id` 포함
- Project 범위에서 유일성 보장: `(project_id, external_user_id)` unique

### 2.3 서버 내부 사용자 ID (Analytics User ID)

- 서버가 관리하는 내부 ID
- `external_user_id`와 1:1 매핑 (Project 범위)
- 익명 사용자도 서버 내부 ID를 가질 수 있음

## 3. 식별 흐름

### 3.1 최초 실행 (비로그인)

```
SDK 초기화
  → 익명 ID 생성 (UUID v4)
  → 영속 저장
  → 이후 이벤트에 anonymous_id 포함
```

### 3.2 로그인 (identify)

```
Analytics.identify("user_1024")
  → SDK: external_user_id를 메모리 + 영속 저장
  → 이후 이벤트에 anonymous_id + external_user_id 모두 포함
  → 서버: external_user_id가 포함된 이벤트 수신
    → analytics_users에서 (project_id, external_user_id) 조회 또는 생성
    → 세션과 이벤트에 analytics_user_id 연결
```

> **현재 구현 상태**: `external_user_id`가 포함된 모든 이벤트에서 `analytics_users` 레코드를 생성/연결한다. `identity_links` 테이블은 DB에 존재하지만 아직 코드에서 사용하지 않는다. 향후 anonymous_id ↔ analytics_user_id 연결 이력 추적이 필요할 때 구현한다.

### 3.3 로그아웃 (resetIdentity)

```
Analytics.resetIdentity()
  → SDK: external_user_id 제거
  → 이후 이벤트에 anonymous_id만 포함
  → 익명 ID는 유지 (새로 생성하지 않음)
```

> **현재 구현 상태**: `resetIdentity()`는 로컬 저장소에서 `external_user_id`를 제거만 한다. 로그아웃 이벤트 자동 전송은 하지 않는다.

### 3.4 계정 전환

```
사용자 A 로그인 → 로그아웃 → 사용자 B 로그인

Analytics.identify("user_A")
  ... 이벤트들 ...
Analytics.resetIdentity()
  ... 익명 이벤트들 ...
Analytics.identify("user_B")
  ... 이벤트들 ...
```

- resetIdentity() 없이 다른 계정으로 identify()하면 이전 연결은 유지되고 새 연결이 추가됨
- 이전 사용자의 이벤트가 다른 사용자에게 귀속되지 않도록 SDK에서 경고 로그 출력

## 4. 소급 귀속 정책

- **로그인 전 이벤트를 로그인 계정에 자동 소급 귀속하지 않는다.**
- 과거 이벤트의 analytics_user_id를 변경하지 않는다.
- 명시적 병합 정책과 계정 전환 보호 장치가 완성된 후에만 소급 병합을 검토한다.

> **현재 구현 상태**: `identity_links` 테이블은 DB에 존재하나 Exposed 테이블 정의 및 생성 로직은 미구현이다. 현재는 `analytics_users` + `analytics_sessions.analytics_user_id`로 사용자 ↔ 세션 연결을 관리한다.

## 5. 프로젝트 간 격리

- 다른 Project의 같은 `external_user_id`는 **동일인으로 취급하지 않는다**.
- `analytics_users` 테이블은 `(project_id, external_user_id)` unique.
- Project A의 "user_1024"와 Project B의 "user_1024"는 별개 레코드.

## 6. 크로스 플랫폼 통합

- 동일 Project 내에서 같은 `external_user_id`로 identify()하면 플랫폼을 넘어 같은 사용자로 연결된다.
- 예: Android에서 identify("user_1024") + Web에서 identify("user_1024") → 같은 analytics_user
- 세션은 Application에 귀속되므로 플랫폼별 세션은 분리 유지

## 7. 세션 관리

### 7.1 세션 시작 조건

- SDK 초기화 시 활성 세션이 없으면 새 세션 시작
- 이전 세션이 타임아웃으로 만료되었으면 새 세션 시작

> **KMP SDK**: 세션 생성 시 `session_start` 이벤트를 자동 전송한다. Web SDK는 향후 구현 예정.

### 7.2 세션 유지

- 이벤트 발생 시마다 세션의 `last_event_at` 갱신
- 백그라운드 전환만으로 즉시 종료하지 않음

### 7.3 세션 종료

- **비활동 타임아웃**: 마지막 이벤트 이후 30분(기본값, 설정 가능) 동안 이벤트 없으면 만료
- **최대 지속 시간**: 24시간 초과 시 무조건 새 세션
- 다음 이벤트 발생 시 이전 세션 만료를 감지하고 새 세션 시작
- `session_end` 전송은 best-effort:
  - 정상 종료: 앱이 포그라운드 복귀 시 타임아웃 감지 → session_end 전송
  - 강제 종료/크래시: session_end 미전송 가능

> **현재 구현 상태**:
> - **KMP SDK**: 세션 만료 감지 시 `session_end`(이전 세션, `duration_ms` 포함)를 전송하고 새 세션의 `session_start`를 전송한다. 백그라운드 진입 시에는 flush만 수행하고, 포그라운드 복귀 시 만료 여부를 판정한다 (업계 표준 패턴).
> - **서버**: `session_end` 이벤트 수신 시 해당 세션의 `ended_at`을 업데이트한다. `session_end`가 오지 않은 경우 대시보드에서 `last_event_at + 30분`으로 종료를 추정한다.
> - **Web SDK**: 세션 타임아웃 판정은 구현되어 있으나 `session_start`/`session_end` 이벤트 자동 전송은 향후 구현 예정이다.

### 7.4 세션 영속화 (업계 표준)

- 세션 ID, 시작 시간, 마지막 이벤트 시간을 **로컬 저장소에 영속화**
  - Android: SharedPreferences
  - iOS: NSUserDefaults
  - Web: localStorage
- 앱이 종료(kill/crash)되더라도 30분 내 재실행 시 **같은 세션 유지**
- 앱 종료 후 30분 초과 시 새 세션 시작
- Firebase Analytics, Amplitude 등 업계 표준과 동일한 정책

### 7.5 세션 ID

- UUID v4로 SDK에서 생성
- 세션 범위에서만 유효
- 새 세션 시작 시 새 ID 생성

### 7.5 세션 설정

| 설정 | 기본값 | 설명 | 구현 상태 |
|---|---|---|---|
| `sessionTimeoutMs` | 1800000 (30분) | 비활동 타임아웃 | 구현 완료 (SDK 설정 가능) |
| `sessionMaxDurationMs` | 86400000 (24시간) | 세션 최대 지속 시간 | KMP SDK 구현 완료 (설정 가능). Web SDK는 24시간 하드코딩 |

## 8. 데이터 모델

### analytics_users
```sql
analytics_users (
  id              UUID PRIMARY KEY,
  project_id      UUID NOT NULL REFERENCES projects(id),
  external_user_id VARCHAR NULL,
  created_at      TIMESTAMP NOT NULL,
  updated_at      TIMESTAMP NOT NULL,
  UNIQUE (project_id, external_user_id) -- external_user_id가 NOT NULL일 때
)
```

### anonymous_identities
```sql
anonymous_identities (
  id              UUID PRIMARY KEY,
  project_id      UUID NOT NULL REFERENCES projects(id),
  anonymous_id    UUID NOT NULL,
  created_at      TIMESTAMP NOT NULL,
  UNIQUE (project_id, anonymous_id)
)
```

### identity_links
```sql
identity_links (
  id                    UUID PRIMARY KEY,
  project_id            UUID NOT NULL REFERENCES projects(id),
  analytics_user_id     UUID NOT NULL REFERENCES analytics_users(id),
  anonymous_identity_id UUID NOT NULL REFERENCES anonymous_identities(id),
  linked_at             TIMESTAMP NOT NULL,
  unlinked_at           TIMESTAMP NULL
)
```

### analytics_sessions
```sql
analytics_sessions (
  id                    UUID PRIMARY KEY,
  project_id            UUID NOT NULL REFERENCES projects(id),
  application_id        UUID NOT NULL REFERENCES applications(id),
  anonymous_id          UUID NOT NULL,
  analytics_user_id     UUID NULL REFERENCES analytics_users(id),
  started_at            TIMESTAMP NOT NULL,
  last_event_at         TIMESTAMP NOT NULL,
  ended_at              TIMESTAMP NULL
)
```
