# Analytics 이벤트 수집 - 온보딩/로그인 화면 정보

## 목차
1. [온보딩 화면 흐름](#1-온보딩-화면-흐름)
2. [로그인 화면 흐름](#2-로그인-화면-흐름)
3. [각 화면별 버튼/액션 목록](#3-각-화면별-버튼액션-목록)
4. [사용자 식별값](#4-사용자-식별값)

---

## 1. 온보딩 화면 흐름

### 전체 흐름도

```
SPLASH → (조건 분기)
├─ 로그인 O + isPorsche=true        → PORSCHE
├─ 로그인 O + isTermsRevision=true  → TERMS_REVISION
├─ 로그인 O + skipOnboard=true      → MAIN
├─ 로그인 O + skipOnboard=false     → TUTORIAL
├─ 게스트 랜딩                       → MAIN (비로그인)
└─ 비로그인                          → ONBOARD
```

### 1-1. SPLASH

| 구분 | 설명 |
|------|------|
| Route | `AppRoute.SPLASH` |
| 파일 | `module/splash/SplashView.kt` |
| 동작 | `chaeviViewModel.init()` 호출 후 조건별 자동 분기 |

| 액션 | 이동 |
|------|------|
| (자동) 앱 업데이트 필요 | 업데이트 다이얼로그 표시 |
| 다이얼로그 "확인" | Play Store 이동 |
| 다이얼로그 "다음에 하기" | 다음 분기 진행 |
| (자동) 초기화 실패 | 에러 다이얼로그 → 앱 종료 |

### 1-2. ONBOARD (메인 진입점)

| 구분 | 설명 |
|------|------|
| Route | `AppRoute.ONBOARD_ROUTE` → `AppRoute.ONBOARD` |
| 파일 | `module/onboard/OnboardView.kt` |
| ViewModel | `OnboardViewModel` |

| 버튼 | 이동 |
|------|------|
| 이메일 로그인 | → LOGIN |
| 이메일 회원가입 | → REGISTER |
| 채비 앱 둘러보기 | → MAIN (게스트 모드) |
| 카카오 로그인 | 소셜 로그인 처리 (아래 참조) |
| 네이버 로그인 | 소셜 로그인 처리 (아래 참조) |
| 구글 로그인 | 소셜 로그인 처리 (아래 참조) |
| 애플 로그인 | → APPLE_LOGIN (WebView) → 결과 처리 |
| Porsche 카드 | → LOGIN (isForPorsche=true) |

**소셜 로그인 결과 분기:**

| 결과 | 이동 |
|------|------|
| 성공 + skipOnboard=true | → MAIN |
| 성공 + skipOnboard=false | → TUTORIAL |
| 미가입 (서버 응답 코드 202) | → REGISTER (SocialData JSON 전달, 정보 자동 채움) |
| 기타 에러 | 에러 메시지 다이얼로그 |

### 1-3. TUTORIAL (7페이지 HorizontalPager)

| 구분 | 설명 |
|------|------|
| Route | `AppRoute.TUTORIAL` |
| 파일 | `module/onboard/tutorial/TutorialView.kt` |

**튜토리얼 페이지 목록:**
1. 소셜 로그인
2. QR, NFC로 간편인증
3. 바로채비 서비스
4. 선점하기
5. 경로상 충전소 찾기
6. 채비 V 멤버스
7. 채비스테이

| 버튼 | 위치 | 이동 |
|------|------|------|
| 건너뛰기 | 상단 우측 (전체 페이지) | → MAIN |
| 다시보지않기 | 각 페이지 | skipOnboard=true 설정 → MAIN |
| 시작하기 | 마지막 페이지 하단 | 체크박스 선택 시 skipOnboard=true → MAIN |

---

## 2. 로그인 화면 흐름

### 2-1. LOGIN (이메일 로그인)

| 구분 | 설명 |
|------|------|
| Route | `AppRoute.LOGIN` + `?email={email}&isForPorsche={isForPorsche}` |
| 파일 | `module/onboard/login/LoginView.kt` |
| ViewModel | `LoginViewModel` |

**입력 필드:**
- 이메일 ID (@ 앞부분)
- 이메일 도메인 (@ 뒷부분, 드롭다운)
- 비밀번호

| 버튼 | 이동 |
|------|------|
| 로그인 | 성공 시 아래 분기 참조 |
| 회원가입 | → REGISTER |
| 아이디 찾기 | → FIND_ID |
| 비밀번호 재설정 | → RESET_PASSWORD |
| 채비 웹사이트 이동 | 외부 브라우저 (chaevi.com) |

**로그인 성공 후 분기:**

| 조건 | 이동 |
|------|------|
| isPorsche=true | → PORSCHE |
| isTermsRevision=true | → TERMS_REVISION |
| skipOnboard=true | → MAIN |
| 그 외 | → TUTORIAL |

**비밀번호 변경 필요 시:** 에러 다이얼로그 → RESET_PASSWORD

### 2-2. REGISTER (회원가입) — 4단계

```
REGISTER_TERM (1/4) → REGISTER_PASS (2/4) → REGISTER_INPUT (3/4) → REGISTER_COMPLETE (4/4) → TUTORIAL
```

| 구분 | 설명 |
|------|------|
| Route | `AppRoute.REGISTER` + `?socialDataJson={socialDataJson}` |
| 파일 | `module/onboard/register/RegisterView.kt` |
| ViewModel | `RegisterViewModel` |

#### Step 1: 약관동의 (REGISTER_TERM)

| 버튼 | 이동 |
|------|------|
| 약관 보기 | → TERM_DETAIL |
| 다음 | → REGISTER_PASS |

#### Step 2: Pass 등록 (REGISTER_PASS)

| 버튼 | 이동 |
|------|------|
| 다음 (결제수단 등록) | → PASS_WEB → REGISTER_INPUT |
| 건너뛰기 | → REGISTER_INPUT |
| 실패 | → ONBOARD_ROUTE |

#### Step 3: 정보입력 (REGISTER_INPUT)

**입력 필드:**
- 이메일 (ID + 도메인)
- 비밀번호
- 비밀번호 확인
- 이름
- 전화번호
- 성별 (남/여)
- 생년월일 (yyyyMMdd)

| 버튼 | 이동 |
|------|------|
| 회원가입 완료 | → REGISTER_COMPLETE |

*소셜 로그인 경유 시 SocialData로 필드 자동 채움*

#### Step 4: 완료 (REGISTER_COMPLETE)

| 버튼 | 이동 |
|------|------|
| 시작하기 | → TUTORIAL (백스택 ONBOARD_ROUTE까지 제거) |

### 2-3. FIND_ID (아이디 찾기)

| 구분 | 설명 |
|------|------|
| Route | `AppRoute.FIND_ID_ROUTE` → `AppRoute.FIND_ID` |
| 파일 | `module/onboard/find_id/FindIdView.kt` |
| ViewModel | `FindIdViewModel` |

**탭 1: 휴대폰번호로 찾기**

| 버튼 | 동작 |
|------|------|
| 인증번호 발송 | 전화번호로 인증번호 SMS 발송 |
| 다음 | 인증번호 확인 → FIND_ID_COMPLETE |

**탭 2: 휴대폰 본인인증하기**

| 버튼 | 동작 |
|------|------|
| 본인인증 | KCB 본인인증 실행 |

**FIND_ID_COMPLETE (결과 화면)**

Route: `find-id/complete?email={email}&createdAt={createdAt}`

| 버튼 | 이동 |
|------|------|
| 비밀번호 재설정 | → RESET_PASSWORD (email 전달) |
| 로그인 | → LOGIN (email 전달) |

### 2-4. RESET_PASSWORD (비밀번호 재설정)

```
RESET_PASSWORD_VERIFY → RESET_PASSWORD_INPUT → LOGIN
```

| 구분 | 설명 |
|------|------|
| Route | `AppRoute.RESET_PASSWORD_ROUTE` → `AppRoute.RESET_PASSWORD_VERIFY` |
| 파일 | `module/onboard/reset_password/ResetPasswordVerifyView.kt` |
| ViewModel | `ResetPasswordVerifyViewModel` |

**RESET_PASSWORD_VERIFY**

탭 1: 이메일로 찾기

| 버튼 | 동작 |
|------|------|
| 인증번호 발송 | 이메일로 인증번호 발송 |
| 다음 | 인증 확인 → RESET_PASSWORD_INPUT |

탭 2: 휴대폰 본인인증하기

| 버튼 | 동작 |
|------|------|
| 본인인증 | KCB 본인인증 실행 |

**RESET_PASSWORD_INPUT**

Route: `reset-password/input?email={email}`

입력 필드: 새 비밀번호, 비밀번호 확인

| 버튼 | 이동 |
|------|------|
| 비밀번호 재설정 | → LOGIN (email 전달) |

---

## 3. 각 화면별 버튼/액션 목록

| 화면 | Route | 버튼/액션 |
|------|-------|----------|
| SPLASH | `splash` | (자동 분기), 앱 업데이트 다이얼로그 [확인, 다음에 하기] |
| ONBOARD | `onboard` | [이메일 로그인], [이메일 회원가입], [앱 둘러보기], [카카오], [네이버], [구글], [애플], [Porsche] |
| LOGIN | `login` | [로그인], [회원가입], [아이디 찾기], [비밀번호 재설정], [채비 웹사이트] |
| TUTORIAL | `tutorial` | [건너뛰기], [다시보지않기], [시작하기] |
| REGISTER_TERM | `register/term` | [약관 보기], [다음] |
| REGISTER_PASS | `register/pass` | [다음], [건너뛰기] |
| REGISTER_INPUT | `register/input` | [회원가입 완료] |
| REGISTER_COMPLETE | `register/complete` | [시작하기] |
| APPLE_LOGIN | `apple-login` | (WebView OAuth 처리) |
| FIND_ID | `find-id` | [인증번호 발송], [다음], [본인인증] |
| FIND_ID_COMPLETE | `find-id/complete` | [비밀번호 재설정], [로그인] |
| RESET_PASSWORD_VERIFY | `reset-password/verify` | [인증번호 발송], [다음], [본인인증] |
| RESET_PASSWORD_INPUT | `reset-password/input` | [비밀번호 재설정] |

---

## 4. 사용자 식별값

### TokenData (로그인/회원가입 성공 시 서버 응답)

```kotlin
data class TokenData(
    val accessToken: String,        // API 호출용 JWT 토큰
    val refreshToken: String,       // 토큰 갱신용
    val memberId: Int,              // 사용자 고유 식별자
    val isPorsche: Boolean?,        // Porsche 회원 여부
    val isTooltip: Boolean,
    val tooltipMsg: String,
    val notes: String,
)
```

### 식별자 접근 방법

| 항목 | 설명 |
|------|------|
| **식별자** | `memberId` (Int) |
| 저장 위치 | `ChaeviViewModel._tokenData: MutableStateFlow<TokenData?>` |
| 접근 (nullable) | `chaeviViewModel.memberIdOrNull(): Int?` |
| 접근 (non-null) | `chaeviViewModel.requireMemberId(): Int` |
| 저장 시점 | 로그인 성공 시 `chaeviViewModel.onUpdateTokenData(tokenData)` |

### MemberInfoData (로그인 후 프로필 정보)

| 필드 | 설명 |
|------|------|
| `userName` | 사용자 이름 |
| `email` | 이메일 |
| `sdkId` | SDK 식별자 |
| `isSocial` | 소셜 로그인 여부 |
| `isTermsRevision` | 약관 개정 필요 여부 |
| `isMembership` | 멤버십 가입 여부 |

### 소셜 로그인 시 수집되는 정보

| 필드 | 설명 |
|------|------|
| `email` | 소셜 계정 이메일 |
| `name` | 이름 |
| `phoneNumber` | 전화번호 |
| `gender` | 성별 ("M" / "F" / "U") |
| `birth` | 생년월일 (YYYYMMDD) |
| `accessToken` | 소셜 액세스 토큰 |
| `refreshToken` | 소셜 리프레시 토큰 |
| `socialType` | 로그인 유형 (KAKAO / GOOGLE / NAVER / APPLE) |

---

## 참고: 주요 파일 경로

| 구분 | 파일 경로 |
|------|----------|
| 스플래시 | `app/src/main/java/com/dym/chaevi/main/module/splash/SplashView.kt` |
| 온보드 | `app/src/main/java/com/dym/chaevi/main/module/onboard/OnboardView.kt` |
| 온보드 ViewModel | `app/src/main/java/com/dym/chaevi/main/module/onboard/OnboardViewModel.kt` |
| 로그인 | `app/src/main/java/com/dym/chaevi/main/module/onboard/login/LoginView.kt` |
| 로그인 ViewModel | `app/src/main/java/com/dym/chaevi/main/module/onboard/login/LoginViewModel.kt` |
| 회원가입 | `app/src/main/java/com/dym/chaevi/main/module/onboard/register/RegisterView.kt` |
| 회원가입 ViewModel | `app/src/main/java/com/dym/chaevi/main/module/onboard/register/RegisterViewModel.kt` |
| 튜토리얼 | `app/src/main/java/com/dym/chaevi/main/module/onboard/tutorial/TutorialView.kt` |
| 아이디 찾기 | `app/src/main/java/com/dym/chaevi/main/module/onboard/find_id/FindIdView.kt` |
| 비밀번호 재설정 | `app/src/main/java/com/dym/chaevi/main/module/onboard/reset_password/ResetPasswordVerifyView.kt` |
| 애플 로그인 | `app/src/main/java/com/dym/chaevi/main/module/onboard/apple_login/AppleLoginView.kt` |
| 네비게이션 | `app/src/main/java/com/dym/chaevi/main/ChaeviNavHost.kt` |
| Route 상수 | `ChaeviNavHost.kt` (AppRoute object) |
| TokenData | `app/src/main/java/com/dym/chaevi/main/model/data/TokenData.kt` |
| MemberInfoData | `app/src/main/java/com/dym/chaevi/main/model/data/MemberInfoData.kt` |
| ChaeviViewModel | `app/src/main/java/com/dym/chaevi/main/ChaeviViewModel.kt` |
