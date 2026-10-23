# Android SDK 연동 가이드

> 버전: 0.1.0
> 대상: Android (Jetpack Compose)

## 1. 의존성 추가

### settings.gradle.kts

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()  // 추가
        google()
        mavenCentral()
    }
}
```

### app/build.gradle.kts

```kotlin
dependencies {
    // Analytics SDK
    implementation("io.github.chaevi.analytics:analytics-kmp-android:0.1.0")
    implementation("io.github.chaevi.analytics:analytics-compose-android:0.1.0")
}
```

Gradle Sync 실행.

## 2. SDK 초기화

### Application 클래스

```kotlin
import io.github.chaevi.analytics.sdk.api.Analytics
import io.github.chaevi.analytics.sdk.api.AnalyticsConfig
import io.github.chaevi.analytics.sdk.data.local.initAndroidStorage

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. 저장소 초기화 (반드시 initialize 전에 호출)
        initAndroidStorage(applicationContext)

        // 2. SDK 초기화
        Analytics.initialize(
            AnalyticsConfig(
                apiKey = "ink_발급받은수집키",
                baseUrl = "http://서버주소:8080",
                appVersion = BuildConfig.VERSION_NAME,
                enableLogging = BuildConfig.DEBUG,  // 디버그 시 로그 출력
            )
        )
    }
}
```

> `apiKey`는 대시보드에서 프로젝트 → 앱 관리 → 수집 키 발급으로 발급.

### AndroidManifest.xml

```xml
<application
    android:name=".MyApp"
    ...>
```

## 3. 생명주기 연동

### MainActivity (또는 단일 Activity)

```kotlin
import io.github.chaevi.analytics.sdk.api.Analytics

class MainActivity : ComponentActivity() {

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
}
```

이 설정으로:
- 백그라운드 진입 시 이벤트 큐 즉시 전송
- 30분 후 복귀 시 이전 세션 종료 + 새 세션 시작

## 4. 화면 전환 자동 추적 (Navigation)

### NavHost 옆에 한 줄 추가

```kotlin
import androidx.navigation.compose.rememberNavController
import io.github.chaevi.analytics.compose.TrackScreenEvents

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    // 이 한 줄로 모든 화면 전환 시 screen_view 이벤트 자동 전송
    TrackScreenEvents(navController)

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("onboard") { OnboardScreen(navController) }
        composable("login") { LoginScreen(navController) }
        // ...
    }
}
```

전송되는 이벤트:
```json
{
  "event_key": "screen_view",
  "properties": { "screen_id": "onboard" }
}
```

`screen_id`는 Navigation route 이름이 자동으로 들어갑니다.

## 5. 버튼 클릭 추적

### AnalyticsButton 사용

```kotlin
import io.github.chaevi.analytics.compose.AnalyticsButton

// 기존 코드
Button(onClick = { onKakaoLogin() }) {
    Text("카카오 로그인")
}

// SDK 적용 — Button을 AnalyticsButton으로 교체
AnalyticsButton(
    buttonId = "onboard_social_kakao",
    onClick = { onKakaoLogin() }
) {
    Text("카카오 로그인")
}
```

전송되는 이벤트:
```json
{
  "event_key": "button_click",
  "properties": { "button_id": "onboard_social_kakao" }
}
```

### button_id 네이밍 규칙

`{화면}_{액션}` 패턴, snake_case:
- `onboard_email_login`
- `login_submit`
- `tutorial_skip`
- `register_term_next`

## 6. 커스텀 이벤트

```kotlin
import io.github.chaevi.analytics.sdk.api.Analytics

// 로그인 성공
Analytics.track("login_success", mapOf(
    "method" to "email"
))

// 소셜 로그인 성공
Analytics.track("social_login_success", mapOf(
    "social_type" to "kakao"
))

// 소셜 로그인 실패
Analytics.track("social_login_fail", mapOf(
    "social_type" to "kakao",
    "error_code" to "NETWORK_ERROR"
))

// 회원가입 단계 진입
Analytics.track("register_step", mapOf(
    "step" to 1,
    "is_social" to false
))
```

## 7. 사용자 식별 (로그인/로그아웃)

```kotlin
// 로그인 성공 시 (이메일/소셜 공통)
Analytics.identify(tokenData.memberId.toString())

// 로그아웃 시
Analytics.resetIdentity()
```

`identify()` 호출 이후 모든 이벤트에 `external_user_id`가 자동 포함됩니다.

## 8. 수동 전송

```kotlin
// 큐에 쌓인 이벤트를 즉시 전송
Analytics.flush()
```

기본적으로 SDK가 주기적으로 자동 전송하므로 대부분 호출 불필요.
중요한 시점(결제 완료 등)에서 확실하게 보내고 싶을 때 사용.

## 9. 자동 수집되는 이벤트

SDK가 별도 코드 없이 자동으로 보내는 이벤트:

| 이벤트 | 시점 |
|--------|------|
| `app_open` | SDK 초기화 시 |
| `session_start` | 새 세션 시작 시 |
| `session_end` | 세션 만료 감지 시 (duration_ms 포함) |
| `screen_view` | Navigation 화면 전환 시 (`TrackScreenEvents` 필요) |
| `button_click` | `AnalyticsButton` 클릭 시 |
| `identify` | `Analytics.identify()` 호출 시 |

## 10. 설정 옵션

```kotlin
AnalyticsConfig(
    apiKey = "ink_...",                    // 필수: 수집 키
    baseUrl = "http://...",               // 필수: 서버 주소
    appVersion = "1.0.0",                 // 선택: 앱 버전 (context에 포함)
    environment = "production",           // 선택: 환경 구분
    enableLogging = false,                // 선택: 디버그 로그 (기본 false)
    sessionTimeoutMs = 1800000,           // 선택: 세션 타임아웃 (기본 30분)
    sessionMaxDurationMs = 86400000,      // 선택: 세션 최대 지속 (기본 24시간)
    flushIntervalMs = 30000,              // 선택: 자동 전송 주기 (기본 30초)
    maxBatchSize = 100,                   // 선택: 배치 최대 이벤트 수
    maxQueueSize = 1000,                  // 선택: 큐 최대 크기
    maxRetries = 3,                       // 선택: 전송 재시도 횟수
    errorListener = { error ->            // 선택: 에러 콜백
        Log.e("Analytics", "${error.code}: ${error.message}")
    },
)
```

## 11. 체크리스트

연동 후 확인 사항:

- [ ] `enableLogging = true`로 설정 후 Logcat에서 `[Analytics/` 로그 확인
- [ ] 앱 시작 시 `session_start`, `app_open` 로그 출력
- [ ] 화면 전환 시 `screen_view` 로그 출력
- [ ] 버튼 클릭 시 `button_click` 로그 출력
- [ ] 대시보드 이벤트 페이지에서 수집된 데이터 확인
- [ ] 사용자 검색에서 identify한 memberId로 검색 가능
