# Taxonomy — 텍소노미 관리

## 1. 개요

텍소노미는 프로젝트에서 사용 가능한 이벤트와 그 속성의 정의 체계다.
시스템 이벤트(공통)와 커스텀 이벤트(프로젝트별)로 구분하며,
서버에서 수신된 이벤트의 구조를 텍소노미 기준으로 검증한다.

## 2. 이벤트 분류

### 2.1 시스템 이벤트

모든 프로젝트에서 기본 제공되는 이벤트. 삭제/비활성화 불가.

| event_key | 설명 | 필수 properties |
|---|---|---|
| `session_start` | 세션 시작 | - |
| `session_end` | 세션 종료 | `duration_ms` (integer) |
| `screen_view` | 화면 조회 | `screen_id` (string) |
| `app_open` | 앱 실행 | - |
| `button_click` | 버튼 클릭 | `button_id` (string) |
| `identify` | 사용자 식별 | `external_user_id` (string) |

### 2.2 커스텀 이벤트

프로젝트별로 관리자가 등록하는 이벤트.

```json
{
  "event_key": "product_view",
  "display_name": "상품 조회",
  "description": "사용자가 상품 상세 페이지를 조회할 때 발생",
  "schema_version": 1,
  "status": "ACTIVE",
  "properties": [
    {
      "property_key": "product_id",
      "value_type": "STRING",
      "required": true,
      "description": "상품 ID"
    },
    {
      "property_key": "category",
      "value_type": "STRING",
      "required": false,
      "description": "상품 카테고리"
    }
  ]
}
```

## 3. 속성 타입

| value_type | 설명 | 예시 |
|---|---|---|
| `STRING` | 문자열 | `"home"` |
| `INTEGER` | 정수 | `42` |
| `FLOAT` | 부동소수점 | `3.14` |
| `BOOLEAN` | 불리언 | `true` |
| `ARRAY` | 배열 (JSON array) | `["a", "b"]` |
| `OBJECT` | 객체 (JSON object) | `{"key": "value"}` |

## 4. 이벤트 상태

| status | 설명 |
|---|---|
| `ACTIVE` | 활성 — 수집 및 분석 대상 |
| `DEPRECATED` | 비권장 — 수집은 허용하나 대시보드에서 경고 표시 |
| `ARCHIVED` | 보관 — 신규 수집 거부, 기존 데이터는 유지 |

## 5. 텍소노미 유일성

- `(project_id, event_key)` 조합은 유일해야 한다.
- event_key는 한번 등록되면 변경할 수 없다 (표시명/설명은 변경 가능).
- schema_version으로 이벤트 구조 변경을 관리한다.

## 6. 미등록 이벤트 정책

SDK에서 텍소노미에 등록되지 않은 event_key를 전송한 경우의 처리 정책.

| 모드 | 동작 | 용도 |
|---|---|---|
| `PERMISSIVE` | 경고 로그 후 수집 허용 | **프로토타입 기본값** |
| `STRICT` | 거부 (400 응답) | 운영 환경 |
| `QUARANTINE` | 별도 격리 저장 (추후 검토) | 전환 과도기 |

- 정책은 프로젝트 설정으로 관리한다.
- 시스템 이벤트는 정책과 무관하게 항상 수집한다.

## 7. 스키마 버전 관리

### 버전 업 시점
- 필수 속성 추가 (breaking change)
- 속성 타입 변경 (breaking change)
- 선택 속성 추가는 버전 업 불필요

### 버전 호환성
- 서버는 현재 버전과 이전 버전 모두 수용
- 이전 버전의 이벤트에 대해 새 필수 속성 검증은 하지 않음
- 검증은 해당 이벤트의 schema_version에 맞는 규칙 적용

## 8. 서버 검증 규칙

수집 API에서 이벤트를 수신할 때:

1. `event_key`가 시스템 이벤트인지 확인 → 시스템 이벤트면 시스템 스키마로 검증
2. `event_key`가 텍소노미에 등록되어 있는지 확인
   - 등록됨: 해당 schema_version의 필수 속성 존재 여부 및 타입 검증
   - 미등록: 프로젝트의 미등록 이벤트 정책에 따라 처리
3. 이벤트 상태 확인
   - `ACTIVE`: 정상 수집
   - `DEPRECATED`: 수집하되 경고 로그
   - `ARCHIVED`: 수집 거부

## 9. 관리 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/admin/projects/{projectId}/taxonomy/events` | 텍소노미 이벤트 목록 |
| `POST` | `/admin/projects/{projectId}/taxonomy/events` | 이벤트 등록 |
| `GET` | `/admin/projects/{projectId}/taxonomy/events/{eventId}` | 이벤트 상세 (속성 포함) |
| `PATCH` | `/admin/projects/{projectId}/taxonomy/events/{eventId}` | 이벤트 수정 (display_name, description, status) |
| `POST` | `/admin/projects/{projectId}/taxonomy/events/{eventId}/properties` | 속성 추가 |
| `PATCH` | `/admin/projects/{projectId}/taxonomy/events/{eventId}/properties/{propertyId}` | 속성 수정 |

## 10. 데이터 모델

### taxonomy_events
```sql
taxonomy_events (
  id              UUID PRIMARY KEY,
  project_id      UUID NOT NULL REFERENCES projects(id),
  event_key       VARCHAR(128) NOT NULL,
  display_name    VARCHAR(256) NOT NULL,
  description     TEXT NULL,
  schema_version  INTEGER NOT NULL DEFAULT 1,
  status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at      TIMESTAMP NOT NULL,
  UNIQUE (project_id, event_key)
)
```

### taxonomy_properties
```sql
taxonomy_properties (
  id                UUID PRIMARY KEY,
  taxonomy_event_id UUID NOT NULL REFERENCES taxonomy_events(id),
  property_key      VARCHAR(128) NOT NULL,
  value_type        VARCHAR(20) NOT NULL,
  required          BOOLEAN NOT NULL DEFAULT false,
  description       TEXT NULL,
  UNIQUE (taxonomy_event_id, property_key)
)
```
