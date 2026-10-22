# Event Contract — 이벤트 계약

## 1. 개요

모든 클라이언트(Android SDK, iOS SDK, Web SDK)와 서버가 동일한 이벤트 스키마를 따른다.
이 문서는 이벤트의 구조, 필수/선택 필드, 배치 전송 형식, 버전 관리 규칙을 정의한다.

## 2. 단일 이벤트 스키마

```json
{
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "session_id": "660e8400-e29b-41d4-a716-446655440000",
  "anonymous_id": "770e8400-e29b-41d4-a716-446655440000",
  "event_key": "screen_view",
  "schema_version": 1,
  "occurred_at": "2026-10-22T10:00:00Z",
  "sequence_number": 42,
  "properties": {
    "screen_id": "home",
    "referrer_screen_id": "splash"
  },
  "context": {
    "app_version": "1.0.0",
    "sdk_version": "0.1.0",
    "os_name": "Android",
    "os_version": "14",
    "device_model": "Pixel 8",
    "device_manufacturer": "Google",
    "locale": "ko_KR",
    "timezone": "Asia/Seoul"
  }
}
```

### 필수 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| `event_id` | UUID (string) | 이벤트 고유 ID. SDK에서 생성. 중복 방지 기준. |
| `session_id` | UUID (string) | 현재 세션 ID |
| `anonymous_id` | UUID (string) | 설치/브라우저 인스턴스별 익명 ID |
| `event_key` | string | 이벤트 종류 (snake_case, 영문) |
| `schema_version` | integer | 이벤트 스키마 버전 (1부터 시작) |
| `occurred_at` | string (ISO-8601 UTC) | 이벤트 발생 시각 (클라이언트 시계 기준) |
| `properties` | object | 이벤트별 속성 (key-value) |
| `context` | object | 디바이스/앱 환경 정보 |

### 선택 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| `sequence_number` | integer | 세션 내 이벤트 순서 번호 |
| `external_user_id` | string | identify() 호출 후 설정되는 로그인 사용자 ID |
| `user_properties` | object | setUserProperties()로 설정된 사용자 속성 |

### 서버가 결정하는 필드 (클라이언트가 보내지 않음)

| 필드 | 설명 |
|---|---|
| `application_id` | ingestion key에서 역산 |
| `project_id` | application → project 관계에서 결정 |
| `platform` | application에 등록된 플랫폼 |
| `received_at` | 서버 수신 시각 |
| `analytics_user_id` | 서버 내부 사용자 ID |

## 3. 배치 전송 형식

SDK는 이벤트를 개별이 아닌 배치로 전송한다.

### 요청

```
POST /ingest/events
Header: X-Ingestion-Key: <ingestion_key>
Content-Type: application/json
```

```json
{
  "sdk_info": {
    "sdk_name": "analytics-kmp-android",
    "sdk_version": "0.1.0"
  },
  "events": [
    { ... },
    { ... }
  ]
}
```

### 응답 (성공)

```json
{
  "data": {
    "accepted": 10,
    "duplicated": 2
  }
}
```

### 응답 (에러)

```json
{
  "success": false,
  "error": {
    "code": "INVALID_INGESTION_KEY",
    "message": "유효하지 않은 수집 키입니다"
  }
}
```

## 4. 공통 시스템 이벤트

모든 프로젝트에서 사용 가능한 기본 이벤트.

| event_key | 설명 | 주요 properties |
|---|---|---|
| `session_start` | 세션 시작 | - |
| `session_end` | 세션 종료 (best-effort) | `duration_ms` |
| `screen_view` | 화면 조회 | `screen_id`, `referrer_screen_id` |
| `app_open` | 앱 실행 | `is_first_open` |
| `button_click` | 버튼 클릭 | `button_id`, `screen_id` |
| `identify` | 사용자 식별 | `external_user_id` |

## 5. 커스텀 이벤트

프로젝트별로 텍소노미에 등록하여 사용한다.

```json
{
  "event_key": "product_view",
  "schema_version": 1,
  "properties": {
    "product_id": "p-123",
    "category": "electronics"
  }
}
```

- event_key는 영문 snake_case 필수
- 텍소노미에 등록된 필수 속성(required properties)은 서버에서 검증
- 미등록 이벤트 정책: 프로토타입 기본값은 **경고 로그 후 수집 허용** (개발 편의). 운영 시 설정으로 거부/격리 전환 가능.

## 6. context 필드 상세

| 필드 | 타입 | 플랫폼 | 설명 |
|---|---|---|---|
| `app_version` | string | 전체 | 호스트 앱 버전 |
| `sdk_version` | string | 전체 | SDK 버전 |
| `os_name` | string | 전체 | Android, iOS, Web |
| `os_version` | string | 전체 | OS 버전 |
| `device_model` | string | Mobile | 기기 모델명 |
| `device_manufacturer` | string | Mobile | 제조사 |
| `locale` | string | 전체 | 로케일 (ko_KR 등) |
| `timezone` | string | 전체 | 타임존 (Asia/Seoul 등) |
| `screen_width` | integer | 전체 | 화면 너비 (px) |
| `screen_height` | integer | 전체 | 화면 높이 (px) |
| `browser_name` | string | Web | 브라우저 이름 |
| `browser_version` | string | Web | 브라우저 버전 |
| `network_type` | string | Mobile | wifi, cellular, unknown |

## 7. 제약 사항

| 항목 | 제한 |
|---|---|
| 배치 최대 이벤트 수 | 100개 |
| 배치 최대 페이로드 크기 | 500KB |
| event_key 최대 길이 | 128자 |
| property key 최대 길이 | 128자 |
| property value 최대 길이 | 2048자 (문자열) |
| properties 최대 개수 | 50개 |
| context 필드 최대 길이 | 256자 |

## 8. 중복 처리 (Idempotency)

- `event_id`를 기준으로 중복을 판별한다.
- 동일 `event_id`가 이미 저장되어 있으면 재저장하지 않고 `duplicated`로 카운트한다.
- SDK는 네트워크 장애 시 같은 배치를 재전송할 수 있으므로 서버는 반드시 idempotent해야 한다.
- HTTP 응답은 `200` (accepted + duplicated 합산)으로 반환한다. `409`는 전체 배치가 중복인 경우에만.

## 9. 시간 처리

- `occurred_at`: 클라이언트 시계 기준 UTC. 클라이언트 시계가 부정확할 수 있음을 감안.
- `received_at`: 서버 시계 기준 UTC. 정렬 및 분석의 보조 기준.
- 저장: 모든 시간은 UTC로 저장.
- 대시보드 표시: 사용자가 선택한 타임존으로 변환 (표시만).

## 10. 버전 관리

- `schema_version`은 이벤트 키 단위로 관리한다.
- 필수 속성 추가 등 breaking change 시 버전을 올린다.
- 서버는 이전 버전도 수용할 수 있어야 한다 (하위 호환).
- 버전별 검증 규칙은 텍소노미에서 관리한다.
