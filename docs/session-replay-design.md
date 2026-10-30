# 세션 리플레이 설계 문서

> 작성일: 2026-10-29
> 최종 수정: 2026-10-30
> 상태: 구현 완료

## 1. 개요

### 1.1 목표
사용자의 세션을 **앱 화면 와이어프레임 기반으로 시각적으로 재생**하는 기능.
세션 리플레이에서 이벤트 흐름에 따라 화면 전환 + 인터랙션 하이라이트를 표시한다.

### 1.2 범위
- **포함**: 앱 화면 렌더링 (React 컴포넌트 + JSON DSL), 세션 타임라인 재생, 인터랙션 하이라이트, 화면 전환 애니메이션
- **제외**: iOS 지원 (추후), 터치/스크롤 좌표 추적 (추후), 실시간 스트리밍

### 1.3 설계 변경 이력

| 시점 | 방식 | 결과 |
|------|------|------|
| 초기 | SDK가 Compose Semantics Tree 자동 캡처 → 서버 전송 | **폐기** — 캡처 품질 한계, Compose 내부 API 의존 |
| 2차 | 관리자가 JSON DSL로 화면 레이아웃 수동 제작 | **보조 수단으로 유지** — 간단한 화면에 적합 |
| 현재 (3차) | React 컴포넌트로 화면 직접 구현 + `data-element-id` 기반 하이라이트 | **주력 방식** — 자유도 높고 인터랙션 표현 용이 |

---

## 2. 아키텍처

### 2.1 전체 흐름

```
┌─────────────────┐     ┌──────────────┐     ┌────────────────────────┐
│  앱 (SDK)        │     │   Backend    │     │  대시보드               │
│                  │     │              │     │                        │
│  trackScreen()   │────→│  이벤트 저장  │────→│  세션 리플레이 뷰어      │
│  track()         │     │  세션 관리    │     │    ↓                   │
│                  │     │              │     │  screen_id 매칭         │
│  screen_view     │     │  screen_     │     │    ↓                   │
│  button_click    │     │  layouts DB  │     │  React 컴포넌트 렌더링   │
│  (element_id)    │     │              │     │  + 인터랙션 하이라이트   │
└─────────────────┘     └──────────────┘     └────────────────────────┘
```

### 2.2 화면 렌더링 방식

세션 리플레이에서 `screen_view` 이벤트를 만나면:

```
screenComponents[screen_id] 존재? → React 컴포넌트 렌더링
                          없으면 → placeholder ("화면 미등록")
```

---

## 3. 화면 렌더링 — React 컴포넌트 방식

### 3.1 개념

앱의 실제 Compose 화면을 참고하여 React 컴포넌트로 와이어프레임을 직접 구현한다.
각 인터랙티브 요소에 `data-element-id`를 부여하여 SDK 이벤트의 `element_id`와 매칭한다.

**장점:**
- CSS/JSX로 자유롭게 레이아웃 구성 → 실제 앱과 유사한 외형
- 상태 관리 가능 (체크박스 토글, 버튼 활성화 등)
- 인터랙션 하이라이트 애니메이션 자연스럽게 적용

**단점:**
- 새 화면 추가 시 React 코드 작성 + 배포 필요

### 3.2 디렉토리 구조

```
web-dashboard/src/features/screens/layouts/
├── index.ts                  # screen_id → 컴포넌트 매핑
├── Highlight.tsx             # 인터랙션 하이라이트 공통 컴포넌트
├── SplashScreen.tsx          # splash
├── OnboardingScreen.tsx      # onboard
├── LoginScreen.tsx           # login
├── FindIdScreen.tsx          # find_id
├── ResetPasswordScreen.tsx   # reset_password
├── RegisterTermScreen.tsx    # register_term
├── RegisterPassScreen.tsx    # register_pass
├── RegisterInputScreen.tsx   # register_input
└── RegisterCompleteScreen.tsx # register_complete
```

### 3.3 screen_id 매핑

```typescript
// layouts/index.ts
export const screenComponents: Record<string, ComponentType<ScreenProps>> = {
  splash: SplashScreen,
  onboard: OnboardingScreen,
  login: LoginScreen,
  find_id: FindIdScreen,
  reset_password: ResetPasswordScreen,
  register_term: RegisterTermScreen,
  register_pass: RegisterPassScreen,
  register_input: RegisterInputScreen,
  register_complete: RegisterCompleteScreen,
};
```

### 3.4 ScreenProps 인터페이스

모든 화면 컴포넌트는 동일한 props를 받는다:

```typescript
export interface ScreenProps {
  highlightId?: string | null;  // 현재 하이라이트할 요소의 element_id
}
```

### 3.5 화면 컴포넌트 작성 규칙

1. 실제 앱의 Compose View 파일을 참고하여 UI 구조를 React로 변환
2. 인터랙티브 요소는 `Highlight` 컴포넌트로 감싸고 `elementId` 부여
3. SDK에서 `Analytics.track("button_click", mapOf("element_id" to "xxx"))` 로 보내는 ID와 일치시킴
4. 폰 프레임(280x700, borderRadius 32) 안에 렌더링

```tsx
// 예시: 온보딩 화면의 버튼
<Highlight elementId="emailLoginButton" activeId={highlightId} style={outlinedBtn}>
  이메일 로그인
</Highlight>
```

### 3.6 등록된 화면 목록 (채비 앱)

| screen_id | 화면명 | 플로우 |
|-----------|--------|--------|
| `splash` | 스플래시 | 온보딩 |
| `onboard` | 온보딩 | 온보딩 |
| `login` | 로그인 | 로그인 |
| `find_id` | 아이디 찾기 | 로그인 |
| `reset_password` | 비밀번호 재설정 | 로그인 |
| `register_term` | 회원가입 — 약관동의 | 회원가입 |
| `register_pass` | 회원가입 — 본인인증 | 회원가입 |
| `register_input` | 회원가입 — 정보입력 | 회원가입 |
| `register_complete` | 회원가입 — 완료 | 회원가입 |

---

## 4. 인터랙션 하이라이트

### 4.1 동작 원리

```
SDK 이벤트: button_click { element_id: "kakaoLogin" }
    ↓
세션 리플레이 재생 중 해당 이벤트 도달
    ↓
highlightId = "kakaoLogin" → 화면 컴포넌트에 prop으로 전달
    ↓
Highlight 컴포넌트가 elementId 매칭 → 파란 테두리 + 글로우 펄스 (0.6초)
```

### 4.2 Highlight 컴포넌트

```tsx
<Highlight elementId="kakaoLogin" activeId={highlightId}>
  <SocialIcon color="#FEE500" label="K" />
</Highlight>
```

- `elementId`: 이 요소의 고유 ID (SDK의 `element_id`와 일치)
- `activeId`: 현재 하이라이트 대상 ID (리플레이에서 전달)
- 매칭 시 파란색 테두리 + 글로우 애니메이션이 0.6초간 표시 후 사라짐

### 4.3 SDK 이벤트 규칙

| 이벤트 | SDK 호출 | 리플레이 동작 |
|--------|----------|--------------|
| 화면 진입 | `Analytics.trackScreen("onboard")` | 해당 React 컴포넌트로 화면 전환 |
| 버튼 클릭 | `Analytics.track("button_click", mapOf("element_id" to "emailLoginButton"))` | 해당 요소 하이라이트 |
| 화면 이탈 | `Analytics.trackScreenLeave("onboard", durationMs)` | 체류시간 표시 |

### 4.4 element_id 네이밍 규칙

```
{기능}{요소타입}
예: emailLoginButton, kakaoLogin, signupLink, nextButton, allAgree
```

- 카멜케이스 사용
- 화면 내에서 고유해야 함
- SDK `track()` 호출과 React 컴포넌트의 `data-element-id`가 정확히 일치해야 함

### 4.5 재생 중 하이라이트 순서

```
화면 A (체류 5초)
  ├─ 0.0초: screen_view → 화면 A 렌더링
  ├─ 1.7초: button_click (element_id: "btn1") → btn1 하이라이트
  ├─ 3.3초: button_click (element_id: "btn2") → btn2 하이라이트
  └─ 5.0초: screen_view → 화면 B로 전환 (페이드+슬라이드 애니메이션)
```

체류시간을 인터랙션 이벤트 수+1로 등분하여 순차적으로 하이라이트한다.

---

## 5. 데이터 모델

### 5.1 DB 스키마 — `screen_layouts` 테이블 (V017)

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

### 5.2 screen_id 매칭 흐름

```
이벤트: screen_view { screen_id: "login" }
    ↓
1) screenComponents["login"] 존재? → LoginScreen 컴포넌트 렌더링
2) 없으면 → screen_layouts에서 JSON 조회 → LayoutWireframe 렌더링
3) 둘 다 없으면 → placeholder 표시
```

---

## 6. Dashboard 구현

### 6.1 페이지 구조

```
web-dashboard/src/features/
├── screens/
│   ├── AppScreensPage.tsx         # 플로우별 화면 미리보기
│   └── layouts/                   # React 컴포넌트 화면들 (9개)
│       ├── index.ts               # screen_id 매핑 + ScreenProps 타입
│       ├── Highlight.tsx          # 하이라이트 공통 컴포넌트
│       └── *Screen.tsx            # 각 화면 컴포넌트
├── replay/
│   ├── SessionListPage.tsx        # 세션 목록 (날짜 필터, 페이지네이션)
│   └── SessionReplayPage.tsx      # 리플레이 뷰어 (화면 + 컨트롤 + 이벤트 패널)
```

### 6.2 앱 화면 미리보기 — 플로우별 그룹핑

화면을 플로우별로 그룹핑하여 표시한다:

```
━━ 온보딩 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
앱 실행 → 스플래시 → 로그인/회원가입 선택
  ①스플래시  →  ②온보딩

━━ 로그인 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
이메일 로그인 → 아이디 찾기 / 비밀번호 재설정
  ①로그인  →  ②아이디 찾기  →  ③비밀번호 재설정

━━ 회원가입 ━━━━━━━━━━━━━━━━━━━━━━━━━━━
약관동의 → 본인인증 → 정보입력 → 완료
  ①약관동의  →  ②본인인증  →  ③정보입력  →  ④완료
```

### 6.3 세션 리플레이 뷰어

```
┌──────────────────────────────────────────────────┐
│  ← 세션 목록   세션 리플레이   abc12345...         │
├────────────────────────────┬─────────────────────┤
│                            │  이벤트 타임라인      │
│   ┌───────────────────┐    │                     │
│   │                   │    │  ● 09:41:00 세션시작 │
│   │   [앱 화면]        │    │  ● 09:41:01 onboard │ ← 파란 바
│   │   React 컴포넌트   │    │  ○ 09:41:03 클릭    │ ← 연한 배경
│   │   또는 와이어프레임  │    │  ○ 09:41:05 클릭    │
│   │                   │    │  ● 09:41:08 login   │
│   └───────────────────┘    │  ...                │
│                            │                     │
│  ○ ○ ● ○ ○  (타임라인 바)  │  디바이스 정보        │
│  ◀  ▶  ▶  1x              │                     │
└────────────────────────────┴─────────────────────┘
```

**기능:**
- 재생 컨트롤: 재생/일시정지, 이전/다음, 속도 (0.5x ~ 4x)
- 화면 전환: 페이드아웃(0.15s) → 슬라이드인(0.2s) 애니메이션
- 이벤트 패널: 현재 화면의 이벤트 하이라이트 (좌측 컬러 바)
- 이벤트 클릭: `screen_view` 클릭 시 해당 화면 이동, `button_click` 클릭 시 해당 요소 하이라이트

---

## 7. SDK 연동 (채비 앱)

### 7.1 초기화

```kotlin
// ChaeviApplication.kt
Analytics.initialize(
    AnalyticsConfig(
        apiKey = "ink_xxx...",
        baseUrl = "http://서버주소:8080",
        appVersion = BuildConfig.VERSION_NAME,
        enableLogging = true,
        flushIntervalMs = 10_000L,
    )
)
```

### 7.2 화면 추적

```kotlin
// 각 화면의 Composable 함수 내부
LaunchedEffect(Unit) { Analytics.trackScreen("onboard") }
```

### 7.3 버튼 클릭 추적

```kotlin
CVButton(
    "이메일 로그인",
    onTap = {
        Analytics.track("button_click", mapOf(
            "screen_id" to "onboard",
            "element_id" to "emailLoginButton",
        ))
        navigateToLogin()
    },
)
```

### 7.4 적용된 화면 목록

| 화면 | trackScreen | button_click 수 |
|------|-------------|----------------|
| OnboardView | `onboard` | 8개 (이메일 로그인, 회원가입, 둘러보기, 카카오/네이버/구글/애플, 포르쉐) |
| LoginView | `login` | 4개 (로그인, 회원가입, 아이디찾기, 비밀번호재설정) |
| FindIdView | `find_id` | — |
| ResetPasswordVerifyView | `reset_password` | — |
| RegisterTermView | `register_term` | 1개 (다음) |
| RegisterPassView | `register_pass` | 1개 (휴대폰 본인인증) |
| RegisterInputView | `register_input` | 1개 (가입하기) |
| RegisterCompleteView | `register_complete` | 1개 (채비 시작하기) |

---

## 8. 폐기된 기능 (SDK 자동 캡처)

아래 기능은 수동 제작 방식으로 대체되어 **폐기**되었다.

### 8.1 제거된 SDK 코드

| 파일 | 설명 |
|------|------|
| `ScreenCapturer.kt` | Compose Semantics 순회 |
| `ScreenCaptureManager.kt` | 버전별 캡처 판정 |
| `HttpTemplateSender.kt` | 템플릿 전송 |
| `AnalyticsConfig.kt` | `enableReplay` 옵션 |

### 8.2 폐기 사유
- Compose Semantics Tree는 레이아웃 컨테이너(배경, 카드, 구분선)를 캡처하지 못함
- 스타일 정보(색상, 폰트, 모서리) 추출이 Compose 내부 API 의존으로 불안정
- **React 컴포넌트 직접 구현이 정확도와 유지보수 면에서 더 우수**

---

## 9. 구현 로드맵

### Phase 1: 세션 타임라인 ✅ 완료
- Backend: 세션 목록 + 타임라인 API
- Dashboard: 세션 목록 + 타임라인 뷰어 + 재생 컨트롤

### Phase 2: 앱 화면 제작 ✅ 완료
- ~~SDK 자동 캡처~~ → 폐기
- JSON DSL 에디터 + LayoutWireframe 렌더러
- React 컴포넌트 방식 도입 (9개 화면)

### Phase 3: 리플레이 연동 + 인터랙션 하이라이트 ✅ 완료
- screen_id → React 컴포넌트 매칭
- Highlight 컴포넌트 + element_id 기반 인터랙션 표시
- 화면 전환 애니메이션 (페이드+슬라이드)
- 이벤트 패널 동기화

### Phase 4: 고도화 (선택)
- 터치 좌표 추적 + 히트맵 오버레이
- 추가 화면 구현 (메인, 충전, 마이페이지 등)
- JSON 포맷 확장 (커스텀 색상, 아이콘 등)
