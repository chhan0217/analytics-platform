# 이벤트 설계서 — 온보딩/로그인

> 작성일: 2026-10-23
> 대상 앱: 채비 (Android, Compose)

## 1. 네이밍 규칙

- `screen_id`: route 이름 기반, snake_case
- `button_id`: `{화면}_{액션}` 패턴, snake_case
- 커스텀 이벤트: `{동사}_{대상}` 패턴

## 2. 자동 수집 (SDK가 처리)

| 이벤트 | 시점 | 비고 |
|--------|------|------|
| `session_start` | 앱 시작 / 세션 만료 후 복귀 | SDK 자동 |
| `session_end` | 세션 만료 감지 시 | SDK 자동 |
| `app_open` | SDK 초기화 시 | SDK 자동 |
| `screen_view` | Navigation 화면 전환 시 | `TrackScreenEvents` 자동 |
| `identify` | `Analytics.identify()` 호출 시 | 로그인 성공 후 직접 호출 |

## 3. 화면별 이벤트 설계

### 3-1. SPLASH

자동 분기되는 화면이라 수집할 버튼 액션 없음. `screen_view`만 자동 수집.

| 이벤트 | 수집 방식 | 비고 |
|--------|-----------|------|
| `screen_view` (splash) | 자동 | Navigation |

### 3-2. ONBOARD (메인 진입점)

| button_id | 버튼 | 이벤트 타입 | 추가 속성 |
|-----------|------|------------|-----------|
| `onboard_email_login` | 이메일 로그인 | `button_click` | - |
| `onboard_email_register` | 이메일 회원가입 | `button_click` | - |
| `onboard_guest_browse` | 앱 둘러보기 | `button_click` | - |
| `onboard_social_kakao` | 카카오 로그인 | `button_click` | - |
| `onboard_social_naver` | 네이버 로그인 | `button_click` | - |
| `onboard_social_google` | 구글 로그인 | `button_click` | - |
| `onboard_social_apple` | 애플 로그인 | `button_click` | - |
| `onboard_porsche` | Porsche 카드 | `button_click` | - |

**커스텀 이벤트 (소셜 로그인 결과):**

| 이벤트 | 시점 | 속성 |
|--------|------|------|
| `social_login_success` | 소셜 로그인 성공 | `social_type`: kakao/naver/google/apple |
| `social_login_fail` | 소셜 로그인 실패 | `social_type`, `error_code` |
| `social_login_register` | 소셜 → 회원가입 전환 | `social_type` |

### 3-3. LOGIN (이메일 로그인)

| button_id | 버튼 | 이벤트 타입 | 추가 속성 |
|-----------|------|------------|-----------|
| `login_submit` | 로그인 | `button_click` | - |
| `login_register` | 회원가입 | `button_click` | - |
| `login_find_id` | 아이디 찾기 | `button_click` | - |
| `login_reset_password` | 비밀번호 재설정 | `button_click` | - |
| `login_website` | 채비 웹사이트 | `button_click` | - |

**커스텀 이벤트:**

| 이벤트 | 시점 | 속성 |
|--------|------|------|
| `login_success` | 로그인 성공 | `method`: email |
| `login_fail` | 로그인 실패 | `method`: email, `error_code` |

### 3-4. TUTORIAL (7페이지)

| button_id | 버튼 | 이벤트 타입 | 추가 속성 |
|-----------|------|------------|-----------|
| `tutorial_skip` | 건너뛰기 | `button_click` | - |
| `tutorial_never_show` | 다시보지않기 | `button_click` | - |
| `tutorial_start` | 시작하기 | `button_click` | - |

**커스텀 이벤트:**

| 이벤트 | 시점 | 속성 |
|--------|------|------|
| `tutorial_page_view` | 튜토리얼 페이지 전환 | `page_index`: 0~6 |
| `tutorial_complete` | 마지막 페이지까지 도달 | - |

### 3-5. REGISTER (회원가입 4단계)

| button_id | 화면 | 버튼 | 추가 속성 |
|-----------|------|------|-----------|
| `register_term_view` | Step 1 | 약관 보기 | - |
| `register_term_next` | Step 1 | 다음 | - |
| `register_pass_next` | Step 2 | 다음 (결제수단) | - |
| `register_pass_skip` | Step 2 | 건너뛰기 | - |
| `register_input_submit` | Step 3 | 회원가입 완료 | - |
| `register_complete_start` | Step 4 | 시작하기 | - |

**커스텀 이벤트:**

| 이벤트 | 시점 | 속성 |
|--------|------|------|
| `register_step` | 각 단계 진입 | `step`: 1~4, `is_social`: true/false |
| `register_success` | 회원가입 완료 | `is_social`: true/false |
| `register_fail` | 회원가입 실패 | `error_code` |

### 3-6. FIND_ID (아이디 찾기)

| button_id | 버튼 | 추가 속성 |
|-----------|------|-----------|
| `find_id_send_code` | 인증번호 발송 | - |
| `find_id_verify` | 다음 (인증확인) | - |
| `find_id_auth` | 본인인증 | - |
| `find_id_reset_password` | 비밀번호 재설정 (결과) | - |
| `find_id_login` | 로그인 (결과) | - |

### 3-7. RESET_PASSWORD (비밀번호 재설정)

| button_id | 버튼 | 추가 속성 |
|-----------|------|-----------|
| `reset_pw_send_code` | 인증번호 발송 | - |
| `reset_pw_verify` | 다음 (인증확인) | - |
| `reset_pw_auth` | 본인인증 | - |
| `reset_pw_submit` | 비밀번호 재설정 | - |

## 4. identify 호출 시점

```kotlin
// 이메일 로그인 성공 시
Analytics.identify(tokenData.memberId.toString())

// 소셜 로그인 성공 시
Analytics.identify(tokenData.memberId.toString())

// 로그아웃 시
Analytics.resetIdentity()
```

`memberId` (Int)를 String으로 변환하여 전달. 모든 로그인 경로(이메일/카카오/네이버/구글/애플)에서 동일하게 호출.

## 5. 연동 코드 예시

### Application 클래스

```kotlin
class ChaeviApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initAndroidStorage(applicationContext)
        Analytics.initialize(AnalyticsConfig(
            apiKey = "ink_발급받은키",
            baseUrl = "https://analytics-api.chaevi.com",
            appVersion = BuildConfig.VERSION_NAME,
            enableLogging = BuildConfig.DEBUG,
        ))
    }
}
```

### NavHost

```kotlin
val navController = rememberNavController()
TrackScreenEvents(navController)  // 이 한 줄로 모든 화면 전환 자동 수집
```

### 온보드 화면

```kotlin
// 기존
Button(onClick = { onKakaoLogin() }) { Text("카카오 로그인") }

// 변경
AnalyticsButton(
    buttonId = "onboard_social_kakao",
    onClick = { onKakaoLogin() }
) { Text("카카오 로그인") }
```

### 소셜 로그인 결과

```kotlin
when (result) {
    is Success -> {
        Analytics.track("social_login_success", mapOf(
            "social_type" to result.socialType.lowercase(),
        ))
        Analytics.identify(result.tokenData.memberId.toString())
    }
    is NeedRegister -> {
        Analytics.track("social_login_register", mapOf(
            "social_type" to result.socialType.lowercase(),
        ))
    }
    is Error -> {
        Analytics.track("social_login_fail", mapOf(
            "social_type" to result.socialType.lowercase(),
            "error_code" to result.errorCode,
        ))
    }
}
```

### 로그인 성공

```kotlin
loginViewModel.login(email, password) { tokenData ->
    Analytics.track("login_success", mapOf("method" to "email"))
    Analytics.identify(tokenData.memberId.toString())
}
```

### MainActivity 생명주기

```kotlin
override fun onStop() {
    super.onStop()
    Analytics.onBackground()
}

override fun onStart() {
    super.onStart()
    if (Analytics.isInitialized()) {
        Analytics.onForeground()
    }
}
```

## 6. 수집 데이터 활용 예시

이 설계로 수집하면 대시보드에서 확인 가능한 것:

| 분석 | 방법 |
|------|------|
| 온보딩 → 로그인 전환율 | `screen_view(onboard)` → `login_success` 퍼널 |
| 소셜 로그인 비율 | `social_login_success`의 `social_type` 속성별 집계 |
| 회원가입 단계별 이탈 | `register_step` 1→2→3→4 퍼널 |
| 튜토리얼 완주율 | `tutorial_page_view` page_index 0→6 퍼널 |
| 아이디 찾기/비번 재설정 빈도 | `button_click` 집계 |
