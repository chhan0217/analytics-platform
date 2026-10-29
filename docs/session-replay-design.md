# 세션 리플레이 설계 문서

> 작성일: 2026-10-29
> 최종 수정: 2026-10-29
> 상태: 구현 중

## 1. 개요

### 1.1 목표
사용자의 세션을 **와이어프레임 기반으로 시각적으로 재생**하는 기능.
관리자가 대시보드에서 앱 화면을 JSON으로 제작하고,
세션 리플레이에서 이벤트 흐름에 따라 화면 전환 + 인터랙션 하이라이트를 표시한다.

### 1.2 범위
- **포함**: 수동 화면 레이아웃 제작, 와이어프레임 재생, 세션 타임라인, 인터랙션 하이라이트
- **제외**: iOS 지원 (추후), 터치/스크롤 좌표 추적 (추후), 실시간 스트리밍

### 1.3 설계 변경 이력

**초기 설계 (폐기)**: SDK가 Compose Semantics Tree를 자동 캡처 → 서버 전송
- 문제점: 캡처 품질 한계 (색상/배경/컨테이너 누락), Compose 내부 API 의존, 스타일 추출 어려움

**현재 설계**: 관리자가 JSON DSL로 화면 레이아웃을 수동 제작
- 장점: 정확한 화면 구조 표현, SDK 의존 없음, 요소별 ID로 인터랙션 매칭 가능
- 단점: 화면별 1회 수동 작업 필요

---

## 2. 아키텍처

```
┌──────────────────┐     ┌───────────────────┐     ┌──────────────────────┐
│  관리자 (대시보드) │     │     Backend        │     │    세션 리플레이       │
│                  │     │                    │     │                      │
│  앱 화면 에디터   │     │  screen_layouts    │     │  이벤트 타임라인       │
│  JSON 작성/저장  │────→│  테이블 저장        │────→│    ↓                  │
│                  │     │                    │     │  screen_id 매칭       │
│  screen_id 별    │     │  세션/이벤트 조회   │     │    ↓                  │
│  레이아웃 관리    │     │                    │     │  와이어프레임 렌더링    │
│                  │     │                    │     │  + 인터랙션 하이라이트  │
└──────────────────┘     └───────────────────┘     └──────────────────────┘
```

---

## 3. 화면 레이아웃 JSON 포맷

### 3.1 기본 구조

```json
{
  "screenId": "mypage",
  "version": 1,
  "root": {
    "id": "root",
    "type": "column",
    "children": [...]
  }
}
```

### 3.2 지원 노드 타입

| 타입 | 설명 | 주요 속성 |
|------|------|-----------|
| `column` | 세로 배치 컨테이너 | `children` |
| `row` | 가로 배치 컨테이너 | `children` |
| `header` | 앱바/헤더 | `children`, `height` |
| `text` | 텍스트 | `value` |
| `button` | 버튼 | `value`, `id` |
| `card` | 카드 컨테이너 | `children`, `value`, `width` |
| `list` | 메뉴 리스트 | `items` (문자열 배열) |
| `navigation` | 하단 네비게이션 | `items` (문자열 배열) |
| `imagePlaceholder` | 이미지 영역 | `value` (설명) |

### 3.3 노드 공통 속성

```json
{
  "id": "login_btn",
  "type": "button",
  "value": "로그인",
  "width": "50%",
  "height": 56
}
```

- `id`: 이벤트 매칭용 고유 식별자 (예: button_click의 button_id와 매칭)
- `type`: 렌더링 타입
- `value`: 표시 텍스트
- `width`: 너비 (px 또는 %)
- `height`: 높이 (px)
- `children`: 자식 노드 배열
- `items`: 리스트/네비게이션 항목 배열

### 3.4 예시 — 로그인 화면

```json
{
  "screenId": "onboard",
  "version": 1,
  "root": {
    "type": "column",
    "children": [
      { "type": "header", "children": [] },
      { "id": "logo", "type": "imagePlaceholder", "value": "CHAEVI" },
      {
        "type": "column",
        "children": [
          { "id": "email_login", "type": "button", "value": "이메일 로그인" },
          { "id": "email_signup", "type": "button", "value": "이메일 회원가입" }
        ]
      },
      { "type": "text", "value": "채비 앱 둘러보기" },
      {
        "type": "row",
        "children": [
          { "id": "kakao", "type": "card", "width": "25%", "value": "카카오" },
          { "id": "naver", "type": "card", "width": "25%", "value": "네이버" },
          { "id": "google", "type": "card", "width": "25%", "value": "구글" },
          { "id": "apple", "type": "card", "width": "25%", "value": "애플" }
        ]
      },
      { "id": "porsche_login", "type": "button", "value": "PORSCHE 로그인" }
    ]
  }
}
```

---

## 4. 데이터 모델

### 4.1 DB 스키마 — `screen_layouts` 테이블 (V017)

```sql
CREATE TABLE screen_layouts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID         NOT NULL REFERENCES projects(id),
    screen_id    VARCHAR(256) NOT NULL,
    display_name VARCHAR(256) NOT NULL,
    layout_json  TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_screen_layouts_project_screen
        UNIQUE (project_id, screen_id)
);
```

### 4.2 screen_id 매칭 규칙

```
이벤트: screen_view { screen_id: "mypage" }
           ↓ 매칭
screen_layouts WHERE screen_id = "mypage"
           ↓
해당 레이아웃 JSON으로 와이어프레임 렌더링
```

### 4.3 인터랙션 매칭 규칙

```
이벤트: button_click { button_id: "email_login" }
           ↓ 매칭
현재 화면 레이아웃에서 id = "email_login" 노드 찾기
           ↓
해당 노드 하이라이트 표시
```

---

## 5. Backend API

### 5.1 세션 API (구현 완료)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/admin/projects/{projectId}/sessions` | 세션 목록 |
| GET | `/admin/projects/{projectId}/sessions/{sessionId}/replay` | 세션 타임라인 |

### 5.2 화면 레이아웃 CRUD (구현 완료)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/admin/projects/{projectId}/screen-layouts` | 레이아웃 목록 |
| POST | `/admin/projects/{projectId}/screen-layouts` | 레이아웃 생성 |
| GET | `/admin/projects/{projectId}/screen-layouts/{layoutId}` | 레이아웃 상세 |
| PATCH | `/admin/projects/{projectId}/screen-layouts/{layoutId}` | 레이아웃 수정 |
| DELETE | `/admin/projects/{projectId}/screen-layouts/{layoutId}` | 레이아웃 삭제 |

---

## 6. Dashboard 구현

### 6.1 페이지 구조

```
web-dashboard/src/features/
├── screens/
│   ├── AppScreensPage.tsx         # 화면 목록 (목록 탭 + 미리보기 탭)
│   ├── ScreenEditPage.tsx         # JSON 에디터 + 미리보기
│   └── components/
│       └── LayoutWireframe.tsx    # JSON → 와이어프레임 렌더러
├── replay/
│   ├── SessionListPage.tsx        # 세션 목록
│   ├── SessionReplayPage.tsx      # 리플레이 뷰어
│   └── components/
│       └── WireframeRenderer.tsx  # SDK 캡처 기반 렌더러 (레거시, 추후 제거)
```

### 6.2 앱 화면 메뉴

- **목록 탭**: 생성된 화면 카드 리스트, 생성/삭제
- **미리보기 탭**: 모든 화면을 와이어프레임으로 나란히 표시
- **편집**: JSON 에디터 (직접 입력 + 파일 업로드) + 실시간 미리보기 + 저장

### 6.3 세션 리플레이 (연동 예정)

```
세션 리플레이에서 screen_view 이벤트 발생 시:
1. screen_id로 screen_layouts에서 레이아웃 조회
2. LayoutWireframe 렌더러로 화면 표시
3. button_click 등 인터랙션 이벤트 시 해당 id 노드 하이라이트
```

---

## 7. 폐기된 기능 (SDK 자동 캡처)

아래 기능은 수동 JSON 에디터 방식으로 대체되어 **폐기**되었다.

### 7.1 제거 대상 — SDK

| 파일 | 설명 |
|------|------|
| `data/local/ScreenCapturer.kt` | expect 선언 |
| `data/local/ScreenCapturer.android.kt` | Compose Semantics 순회 actual |
| `data/local/ScreenCapturer.ios.kt` | iOS stub |
| `domain/usecase/ScreenCaptureManager.kt` | 버전별 캡처 판정 |
| `data/remote/HttpTemplateSender.kt` | 템플릿 전송 |
| `api/Analytics.kt` | `tryCaptureScreen()` 등 캡처 연동 코드 |
| `api/AnalyticsConfig.kt` | `enableReplay` 옵션 |

### 7.2 제거 대상 — Backend

| 파일/테이블 | 설명 |
|------------|------|
| `POST /ingest/screen-templates` | 템플릿 수집 엔드포인트 |
| `ScreenTemplateService.kt` | 템플릿 수집 로직 |
| `screen_templates` 테이블 | SDK 캡처 데이터 저장 (보류, 추후 삭제) |

### 7.3 폐기 사유
- Compose Semantics Tree는 레이아웃 컨테이너(배경, 카드, 구분선)를 캡처하지 못함
- 스타일 정보(색상, 폰트, 모서리) 추출이 Compose 내부 API 의존으로 불안정
- 스크린샷 방식은 스크롤 문제가 있음
- **수동 JSON 제작이 정확도와 유지보수 면에서 더 우수**

---

## 8. 구현 로드맵

### Phase 1: 세션 타임라인 ✅ 완료
- Backend: 세션 목록 + 타임라인 API
- Dashboard: 세션 목록 + 타임라인 뷰어 + 재생 컨트롤

### Phase 2: SDK 자동 캡처 ❌ 폐기 → 수동 JSON 에디터로 대체
- ~~SDK Compose Semantics 캡처~~
- Dashboard: 앱 화면 CRUD + JSON 에디터 + 미리보기

### Phase 3: 리플레이 연동 (진행 예정)
- 세션 리플레이에서 screen_layouts 데이터로 화면 표시
- 인터랙션 하이라이트 (button_click → 해당 버튼 강조)

### Phase 4: 고도화 (선택)
- 터치 좌표 추적 + 히트맵 오버레이
- 화면 전환 애니메이션
- JSON 포맷 확장 (커스텀 색상, 아이콘 등)

---

## 9. 인터랙션 하이라이트 설계 (Phase 3)

### 9.1 매칭 방식

```
이벤트 properties의 키           와이어프레임 노드의 id
─────────────────────          ──────────────────
button_id: "email_login"   →   id: "email_login"
list_item: "구독 상품"      →   items 배열 내 매칭
screen_id: "mypage"        →   screenId: "mypage"
```

### 9.2 하이라이트 표시

```
┌──────────────────────┐
│                      │
│  ┌────────────────┐  │
│  │ 이메일 로그인   │◀── 클릭됨: 파란색 테두리 + 펄스 애니메이션
│  └────────────────┘  │
│  ┌────────────────┐  │
│  │ 이메일 회원가입  │  │
│  └────────────────┘  │
│                      │
└──────────────────────┘
```

### 9.3 지원 이벤트 타입

| 이벤트 | 매칭 키 | 효과 |
|--------|---------|------|
| `screen_view` | `screen_id` | 화면 전환 |
| `button_click` | `button_id` | 버튼 하이라이트 |
| `list_item_click` | `item_id` 또는 `item_name` | 리스트 항목 하이라이트 |
| `screen_leave` | `screen_id` | 체류시간 표시 |
