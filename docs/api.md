# API 설계 — `/api/v1`

## 1. 공통 규칙

- **URL 경로**: kebab-case (`/admin/taxonomy-events`)
- **JSON 필드**: snake_case (`event_key`, `project_id`)
- **날짜**: ISO-8601 UTC (`2026-10-22T10:00:00Z`)
- **인증**: 수집 API는 `X-Ingestion-Key` 헤더, 관리자 API는 인증 토큰 (로컬 개발 시 간소화)

### 성공 응답 (단건)
```json
{
  "data": { ... }
}
```

### 성공 응답 (리스트 — offset 페이지네이션)
```json
{
  "data": [ ... ],
  "meta": {
    "total": 150,
    "page": 1,
    "page_size": 20
  }
}
```

### 성공 응답 (리스트 — cursor 페이지네이션)
```json
{
  "data": [ ... ],
  "meta": {
    "next_cursor": "eyJpZCI6MTAwfQ==",
    "has_more": true
  }
}
```

### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "INVALID_EVENT_KEY",
    "message": "이벤트 키가 텍소노미에 등록되지 않았습니다",
    "details": {}
  }
}
```

### HTTP 상태 코드

| 상태 | 의미 |
|---|---|
| `200` | 성공 |
| `201` | 생성 성공 |
| `400` | 입력 검증 실패 |
| `401` | 인증 실패 |
| `403` | 권한 없음 |
| `404` | 리소스 없음 |
| `409` | 중복 |
| `413` | 페이로드 초과 |
| `500` | 서버 내부 오류 |

---

## 2. 인증 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/admin/auth/login` | 관리자 로그인 |

### 요청
```json
{
  "email": "admin@analytics.local",
  "password": "admin1234"
}
```

### 응답 (200)
```json
{
  "data": {
    "token": "<JWT 토큰>"
  }
}
```

> 로컬 개발 환경에서는 시드 관리자 계정(`admin@analytics.local` / `admin1234`)이 자동 생성된다.

---

## 3. Health

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/health` | 서버 상태 확인 |

### 응답
```json
{
  "data": {
    "status": "ok",
    "timestamp": "2026-10-22T10:00:00Z"
  }
}
```

---

## 3. 수집 API (Ingest)

인증: `X-Ingestion-Key` 헤더

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/ingest/events` | 이벤트 배치 수집 |

### 요청
```json
{
  "sdk_info": {
    "sdk_name": "analytics-kmp-android",
    "sdk_version": "0.1.0"
  },
  "events": [
    {
      "event_id": "uuid",
      "session_id": "uuid",
      "anonymous_id": "uuid",
      "event_key": "screen_view",
      "schema_version": 1,
      "occurred_at": "2026-10-22T10:00:00Z",
      "sequence_number": 1,
      "properties": { "screen_id": "home" },
      "context": { "app_version": "1.0.0", "sdk_version": "0.1.0" }
    }
  ]
}
```

### 응답 (200)
```json
{
  "data": {
    "accepted": 8,
    "duplicated": 2
  }
}
```

### 에러 코드

| code | 상태 | 설명 |
|---|---|---|
| `INVALID_INGESTION_KEY` | 401 | 유효하지 않은 수집 키 |
| `REVOKED_INGESTION_KEY` | 401 | 폐기된 수집 키 |
| `PAYLOAD_TOO_LARGE` | 413 | 배치 크기 초과 |
| `INVALID_EVENT_SCHEMA` | 400 | 이벤트 스키마 검증 실패 |
| `UNREGISTERED_EVENT` | 400 | 미등록 이벤트 (STRICT 모드) |

---

## 4. 관리자 API — 조직/프로젝트/앱

인증: 관리자 토큰

### 조직 (Organization)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/admin/organizations` | 조직 생성 |
| `GET` | `/admin/organizations` | 조직 목록 |
| `GET` | `/admin/organizations/{organizationId}` | 조직 상세 |
| `PATCH` | `/admin/organizations/{organizationId}` | 조직 수정 |

### 프로젝트 (Project)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/admin/organizations/{organizationId}/projects` | 프로젝트 생성 |
| `GET` | `/admin/organizations/{organizationId}/projects` | 프로젝트 목록 |
| `GET` | `/admin/projects/{projectId}` | 프로젝트 상세 |
| `PATCH` | `/admin/projects/{projectId}` | 프로젝트 수정 |

### 애플리케이션 (Application)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/admin/projects/{projectId}/applications` | 앱 등록 |
| `GET` | `/admin/projects/{projectId}/applications` | 앱 목록 |
| `GET` | `/admin/projects/{projectId}/applications/{applicationId}` | 앱 상세 |
| `PATCH` | `/admin/projects/{projectId}/applications/{applicationId}` | 앱 수정 |

### 앱 자격 증명 (App Credentials)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/admin/projects/{projectId}/applications/{applicationId}/credentials` | 수집 키 발급 |
| `GET` | `/admin/projects/{projectId}/applications/{applicationId}/credentials` | 수집 키 목록 |
| `DELETE` | `/admin/projects/{projectId}/applications/{applicationId}/credentials/{credentialId}` | 수집 키 폐기 (204 No Content) |

---

## 5. 관리자 API — 텍소노미

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/admin/projects/{projectId}/taxonomy/events` | 이벤트 등록 |
| `GET` | `/admin/projects/{projectId}/taxonomy/events` | 이벤트 목록 |
| `GET` | `/admin/projects/{projectId}/taxonomy/events/{eventId}` | 이벤트 상세 |
| `PATCH` | `/admin/projects/{projectId}/taxonomy/events/{eventId}` | 이벤트 수정 |
| `POST` | `/admin/projects/{projectId}/taxonomy/events/{eventId}/properties` | 속성 추가 |
| `PATCH` | `/admin/projects/{projectId}/taxonomy/events/{eventId}/properties/{propertyId}` | 속성 수정 |

---

## 6. 관리자 API — 이벤트 조회

페이지네이션: **cursor 기반** (대량 데이터)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/admin/projects/{projectId}/events` | 이벤트 목록 조회 |
| `GET` | `/admin/projects/{projectId}/events/{eventId}` | 이벤트 상세 (JSON 전체) |

### 쿼리 파라미터 (`/events`)

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `start_date` | ISO-8601 | 시작일 (필수) |
| `end_date` | ISO-8601 | 종료일 (필수) |
| `platform` | enum | ANDROID, IOS, WEB |
| `application_id` | UUID | 특정 앱 필터 |
| `event_key` | string | 이벤트 종류 필터 |
| `user_id` | UUID | analytics_user_id 필터 |
| `session_id` | UUID | 세션 필터 |
| `cursor` | string | 페이지네이션 커서 |
| `page_size` | integer | 페이지 크기 (기본 50, 최대 200) |

---

## 7. 관리자 API — 사용자 조회

페이지네이션: **offset 기반** (소량 결과)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/admin/projects/{projectId}/users` | 사용자 검색 |
| `GET` | `/admin/projects/{projectId}/users/{userId}/sessions` | 사용자의 세션 목록 |
| `GET` | `/admin/projects/{projectId}/sessions/{sessionId}/events` | 세션의 이벤트 타임라인 |

### 사용자 검색 파라미터 (`/users`)

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `external_user_id` | string | 로그인 ID로 검색 |
| `anonymous_id` | UUID | 익명 ID로 검색 |
| `page` | integer | 페이지 번호 (기본 1) |
| `page_size` | integer | 페이지 크기 (기본 20) |

---

## 8. 관리자 API — 분석

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/admin/projects/{projectId}/analytics/overview` | 개요 (이벤트 수, 세션 수, 사용자 수) |
| `GET` | `/admin/projects/{projectId}/analytics/screens` | 화면별 통계 |
| `GET` | `/admin/projects/{projectId}/analytics/journeys` | 화면 전이 집계 |
| `POST` | `/admin/projects/{projectId}/analytics/funnels/query` | 퍼널 분석 |
| `GET` | `/admin/projects/{projectId}/analytics/events/summary` | 이벤트별 집계 (횟수, 사용자 수, 세션 수) |
| `GET` | `/admin/projects/{projectId}/analytics/events/breakdown` | 속성별 분석 (event_key + property_key 기준) |
| `GET` | `/admin/projects/{projectId}/analytics/retention` | 리텐션 분석 (type=new/active, retention_days=1,7,30) |

### 공통 쿼리 파라미터

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `start_date` | ISO-8601 | 시작일 (필수) |
| `end_date` | ISO-8601 | 종료일 (필수) |
| `platform` | enum | 플랫폼 필터 |
| `application_id` | UUID | 앱 필터 |

### 퍼널 분석 요청 (POST `/funnels/query`)

```json
{
  "start_date": "2026-10-01T00:00:00Z",
  "end_date": "2026-10-22T23:59:59Z",
  "steps": [
    { "event_key": "screen_view", "filter": { "screen_id": "product_list" } },
    { "event_key": "product_view" },
    { "event_key": "add_to_cart" },
    { "event_key": "purchase_completed" }
  ],
  "scope": "SESSION",
  "platform": "ANDROID"
}
```

### 퍼널 분석 응답

```json
{
  "data": {
    "steps": [
      { "step": 1, "event_key": "screen_view", "unique_users": 1000, "unique_sessions": 1200 },
      { "step": 2, "event_key": "product_view", "unique_users": 800, "unique_sessions": 900 },
      { "step": 3, "event_key": "add_to_cart", "unique_users": 300, "unique_sessions": 350 },
      { "step": 4, "event_key": "purchase_completed", "unique_users": 100, "unique_sessions": 110 }
    ],
    "overall_conversion_rate": 0.10,
    "scope": "SESSION"
  }
}
```

---

## 9. 에러 코드 체계

### 공통
| code | 설명 |
|---|---|
| `UNAUTHORIZED` | 인증 실패 |
| `FORBIDDEN` | 권한 없음 |
| `NOT_FOUND` | 리소스 없음 |
| `VALIDATION_ERROR` | 입력 검증 실패 |
| `INTERNAL_ERROR` | 서버 내부 오류 |

### 수집
| code | 설명 |
|---|---|
| `INVALID_INGESTION_KEY` | 유효하지 않은 수집 키 |
| `REVOKED_INGESTION_KEY` | 폐기된 수집 키 |
| `PAYLOAD_TOO_LARGE` | 페이로드 크기 초과 |
| `INVALID_EVENT_SCHEMA` | 이벤트 스키마 불일치 |
| `UNREGISTERED_EVENT` | 미등록 이벤트 (STRICT 모드) |
| `ARCHIVED_EVENT` | 보관된 이벤트 (수집 거부) |

### 관리
| code | 설명 |
|---|---|
| `DUPLICATE_ORGANIZATION` | 조직명 중복 |
| `DUPLICATE_PROJECT` | 프로젝트명 중복 |
| `DUPLICATE_EVENT_KEY` | 이벤트 키 중복 |
| `DUPLICATE_PROPERTY_KEY` | 속성 키 중복 |
| `INVALID_PLATFORM` | 유효하지 않은 플랫폼 |
| `EVENT_KEY_IMMUTABLE` | 이벤트 키 변경 불가 |
