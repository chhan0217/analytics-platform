# Chaevi 앱 Analytics 텍소노미 설계서

## 1. 텍소노미란?

앱에서 일어나는 모든 일(화면 이동, 버튼 클릭, 로그인 등)의 **이름을 어떻게 짓고 분류할 것인가**에 대한 규칙서다.

### 왜 필요한가?

텍소노미 없이 각자 마음대로 이벤트를 만들면:

```
// 개발자 A
Analytics.track("btn_click", mapOf("name" to "login"))

// 개발자 B
Analytics.track("button_click", mapOf("button_id" to "login_submit"))

// 개발자 C
Analytics.track("login_button_pressed", mapOf())
```

세 개 다 "로그인 버튼 눌렀다"인데, 데이터 분석할 때 세 군데를 합쳐야 한다.
시간이 지나면 아무도 뭐가 뭔지 모른다.

텍소노미는 이걸 방지하는 **"데이터의 사전"**이다:
- **이벤트 이름**: 어떤 패턴으로 짓는가 (`{대상}_{행위}`)
- **속성(property)**: 어떤 추가 정보를 붙이는가 (`button_id`, `screen_name`)
- **화면 이름**: 각 화면을 뭐라고 부르는가

### 텍소노미의 구성 요소

```
텍소노미
├── 네이밍 규칙 .............. 이름을 어떻게 짓는가
├── 화면(Screen) 분류 ........ 각 화면의 고유 이름
├── 이벤트(Event) 분류 ....... 무슨 일이 일어났는가
│   ├── 자동 수집 이벤트 ..... SDK가 알아서 보내는 것
│   └── 수동 추적 이벤트 ..... 개발자가 코드로 보내는 것
└── 속성(Property) 정의 ...... 이벤트에 딸린 상세 정보
```

---

## 2. 현재 상태

### 사용 중인 Analytics 도구

| 도구 | 용도 | 상태 |
|------|------|------|
| Chaevi Analytics SDK (`io.github.chaevi.analytics`) | 이벤트/화면 추적 | onboard/login만 적용 |
| Datadog | RUM, Session Replay, 네트워크 모니터링 | 초기화 완료, 자동 추적 |
| Airbridge | 딥링크, 그로스 마케팅 | 딥링크 처리만 구현 |

### 현재 수집 중인 이벤트 (총 11건)

| 이벤트명 | 속성 | 파일 |
|----------|------|------|
| `login_success` | `method` | LoginViewModel.kt |
| `login_fail` | `method`, `error_code` | LoginViewModel.kt |
| `social_login_success` | `social_type` | OnboardViewModel.kt |
| `social_login_fail` | `social_type`, `error_code` | OnboardViewModel.kt |
| `social_login_register` | `social_type` | OnboardViewModel.kt |
| `button_click` | `button_id` (14종) | OnboardView.kt, LoginView.kt |

### 현재 수집 중인 사용자 식별 (총 2건)

| 호출 | 파일 |
|------|------|
| `Analytics.identify(memberId)` | LoginViewModel.kt |
| `Analytics.identify(memberId)` | OnboardViewModel.kt |

### 기존 SCREEN/CATEGORY/LABEL enum

정의만 있고 **사용처 0건**. 삭제 권장.

---

## 3. 네이밍 규칙

### 이벤트명

```
{대상}_{행위}
```

- snake_case
- 대상이 먼저, 행위가 뒤에
- 예: `login_success`, `card_register`, `charging_start`

기존에 `login_success`, `button_click` 패턴이 이미 사용 중이므로 이 패턴을 유지한다.

### 화면명 (screen_id)

```
AppRoute 경로를 그대로 사용한다.
```

- `TrackScreenEvents(navController)`가 Navigation route를 `screen_id`로 자동 전송
- **파라미터는 SDK에서 자동 제거**: `?query=...`와 `/{pathParam}`은 잘라낸다
- 별도의 이름 매핑 없이 route 값 자체가 screen_id가 된다

예시:
```
route 정의: AppRoute.LOGIN + "?email={email}&isForPorsche={isForPorsche}"
수집되는 screen_id: "login"

route 정의: AppRoute.MY_PAGE_ANNOUNCEMENT + "?announcementKey={announcementKey}"
수집되는 screen_id: "main/my-page/announcement"

route 정의: AppRoute.MY_PAGE_ANNOUNCEMENT_DETAIL + "/{announcementItem}"
수집되는 screen_id: "main/my-page/announcement/detail"
```

같은 AppRoute에 파라미터 유무로 두 개의 composable이 등록된 경우(일반 진입 / 푸시 진입 등)는 **같은 화면**이므로 같은 screen_id로 합쳐진다.

### 속성 키

```
snake_case
```

- 예: `button_id`, `screen_name`, `station_id`, `error_code`

### 플랫폼별 속성 값

텍소노미(이벤트 + 속성)는 **프로젝트 단위로 한 벌만 관리**한다.
Android/iOS/Web에서 속성 값(예: `button_id`)이 다를 수 있으며, 대시보드에서 플랫폼 필터로 구분한다.

```
Android: { "button_id": "login_submit" }
iOS:     { "button_id": "btnLogin" }
→ 같은 button_click 이벤트, 같은 button_id 속성. 값만 플랫폼별로 다름.
```

---

## 4. 화면(Screen) 텍소노미

AppRoute에 정의된 route 상수 기반. `screen_id`는 파라미터 제거 후의 route 경로 그대로다.

### Splash / Terms Revision

| screen_id | 설명 |
|-----------|------|
| `splash` | 스플래시 |
| `terms-revision` | 약관개정 동의 |
| `terms-revision/detail` | 약관개정 상세 |
| `terms-revision/result` | 약관개정 결과 |

### Onboard (인증)

| screen_id | 설명 |
|-----------|------|
| `onboard` | 온보드 메인 |
| `login` | 이메일 로그인 |
| `apple-login` | 애플 로그인 웹뷰 |
| `register` | 회원가입 진입 |
| `register/term` | 회원가입 약관동의 |
| `register/pass` | 회원가입 PASS 인증 |
| `register/verify` | 회원가입 본인인증 |
| `register/input` | 회원가입 정보입력 |
| `register/check` | 회원가입 정보확인 |
| `register/input-more-1` | 회원가입 추가정보1 |
| `register/input-more-2` | 회원가입 추가정보2 |
| `register/complete` | 회원가입 완료 |
| `find-id` | 아이디 찾기 |
| `find-id/complete` | 아이디 찾기 결과 |
| `reset-password/verify` | 비밀번호 재설정 인증 |
| `reset-password/input` | 비밀번호 재설정 입력 |
| `tutorial` | 튜토리얼 |

### Home (홈)

| screen_id | 설명 |
|-----------|------|
| `main/home` | 홈 메인 |
| `main/home/qr` | QR 스캔 |
| `main/home/auth-nfc` | NFC 인증 |
| `main/home/near-charger` | 근처 충전기 목록 |
| `main/home/near-charger-connect` | 근처 충전기 연결 |
| `main/home/notice` | 알림 목록 |
| `main/home/delete-notice` | 알림 삭제 |
| `main/home/coupon-notice` | 알림 쿠폰 |
| `main/home/badge-list` | 배지 목록 |
| `main/home/charging-info` | 충전 상태 |
| `main/home/charging-info/pnc` | PnC 등록 |
| `main/home/charging-info/auto-cancel-pnc` | PnC 자동취소 예약 |
| `main/home/hyundai-home-charger` | 현대 홈충전기 |
| `main/home/hyundai-home-charger/usage` | 현대 홈충전기 사용량 |
| `main/home/hyundai-home-charger/history` | 현대 홈충전기 이력 |
| `main/home/hyundai-home-charger/setting` | 현대 홈충전기 설정 |
| `main/home/hyundai-home-charger/info` | 현대 홈충전기 정보 |

### Charge Station (충전소)

| screen_id | 설명 |
|-----------|------|
| `main/charge-station` | 충전소 지도 메인 |
| `main/charge-station/search` | 충전소 검색 |
| `main/charge-station/favor-list` | 즐겨찾기 충전소 |
| `main/charge-station/preoccupy` | 충전소 선점 |
| `main/charge-station/chaevi-stays` | 채비스테이 |
| `main/charge-station/search-route` | 경로 탐색 |

### Membership (멤버십)

| screen_id | 설명 |
|-----------|------|
| `main/membership` | 멤버십 메인 |
| `main/membership/register` | 회원카드 등록 |
| `main/membership/register/success` | 회원카드 등록 성공 |
| `main/membership/register-card` | 카드 등록 선택 |
| `main/membership/register-card/general` | 일반 카드 등록 |
| `main/membership/register-card/select-vehicle` | 교통카드 차량 선택 |
| `main/membership/register-card/term` | 교통카드 약관 |
| `main/membership/register-card/additional-info` | 교통카드 추가정보 |
| `main/membership/manage-card` | 카드 관리 |
| `main/membership/move-credit` | 크레딧 이동 |
| `main/membership/refund-credit` | 크레딧 환불 |
| `main/membership/auth-user` | 크레딧 인증 |
| `main/membership/apply-card` | 카드 발급 신청 |
| `main/membership/enter-address` | 카드 발급 주소 |
| `main/membership/final-confirm` | 카드 발급 최종 확인 |
| `main/membership/manage-credit` | 크레딧 관리 |
| `main/membership/manage-credit/detail` | 크레딧 내역 상세 |
| `main/membership/manage-credit/mileage-credit` | 마일리지 크레딧 |
| `main/membership/manage-credit/expiring-credit` | 소멸예정 크레딧 |
| `main/membership/unpaid-records` | 미납 내역 |
| `main/membership/charge-credit` | 크레딧 충전 |
| `main/membership/charge-credit/set-auto-charge` | 자동충전 설정 |
| `main/membership/usage-detail` | 이용 내역 |
| `main/membership/usage-detail/charge-detail` | 충전 내역 상세 |
| `main/membership/usage-detail/download` | 이용내역 다운로드 |
| `main/membership/usage-detail/occupy-detail` | 점유 내역 상세 |
| `main/membership/register-coupon` | 쿠폰 등록 |
| `main/membership/query-issued-card` | 발급카드 조회 |
| `main/membership/biz-pass-list` | 비즈패스 목록 |
| `main/membership/biz-pass-list/register` | 비즈패스 등록 |
| `main/membership/manage-chaevi-pass` | 채비패스 관리 |
| `main/membership/manage-chaevi-pass/change` | 채비패스 변경 |
| `main/membership/manage-chaevi-pass/change-biz` | 비즈패스 변경 |
| `main/membership/manage-chaevi-pass/unsubscribe` | 채비패스 해지 |
| `main/membership/manage-v-members` | V멤버스 관리 |
| `main/membership/manage-v-members/unsubscribe` | V멤버스 해지 |
| `main/membership/subscription_prize` | 구독 경품 |

### My Page (마이페이지)

| screen_id | 설명 |
|-----------|------|
| `main/my-page` | 마이페이지 메인 |
| `main/my-page/edit-my-info` | 내 정보 수정 |
| `main/my-page/edit-my-info/check-pwd` | 비밀번호 확인 |
| `main/my-page/edit-my-info/reset-pwd` | 새 비밀번호 입력 |
| `main/my-page/edit-my-info/change-phone-number` | 전화번호 변경 |
| `main/my-page/edit-my-info/edit-my-info` | 회원탈퇴 |
| `main/my-page/manage-payment` | 결제수단 관리 |
| `main/my-page/setting` | 설정 |
| `main/my-page/setting/term` | 약관 목록 |
| `main/my-page/setting/password` | 간편비밀번호 설정 |
| `main/my-page/setting/smart-control` | 스마트 충전 설정 |
| `main/my-page/my-car` | 마이카 |
| `main/my-page/my-car/list` | 마이카 목록 |
| `main/my-page/my-car/management` | 마이카 연동 관리 |
| `main/my-page/my-car/connect` | 마이카 연동 |
| `main/my-page/my-car/connect/login` | 마이카 연동 로그인 |
| `main/my-page/transfer-point` | 포인트 전환 |
| `main/my-page/verify-purchase` | 구매인증 목록 |
| `main/my-page/verify-purchase/t4k` | T4K 구매인증 |
| `main/my-page/verify-purchase/kia` | KIA 구매인증 |
| `main/my-page/verify-purchase/vitesco` | Vitesco 구매인증 |
| `main/my-page/announcement` | 공지사항 |
| `main/my-page/announcement/detail` | 공지사항 상세 |
| `main/my-page/event` | 이벤트 |
| `main/my-page/event/detail` | 이벤트 상세 |
| `main/my-page/gift` | 선물하기 |
| `main/my-page/chaevi-info` | 채비 정보 |
| `main/my-page/register-membership` | 제휴 멤버십 등록 |
| `main/my-page/register-membership/detail` | 제휴 멤버십 상세 |
| `main/my-page/event-register-membership` | 이벤트 제휴 멤버십 |
| `main/my-page/car-management-list` | 차량 관리 |
| `main/my-page/car-management-list/edit` | 차량 정보 수정 |
| `main/my-page/car-management-list/registration` | 차량 등록 |
| `main/my-page/customer-support` | 고객지원 |
| `main/my-page/customer-support/faq` | FAQ |
| `main/my-page/customer-support/one-on-one-inquiry` | 1:1 문의 |
| `main/my-page/battery` | 배터리 |
| `main/my-page/offerwall` | 오퍼월 |
| `main/my-page/ble-home-charger` | BLE 홈충전기 |

### Common / External

| screen_id | 설명 |
|-----------|------|
| `term` | 약관 상세 웹뷰 |
| `web` | 범용 웹뷰 |
| `landing-web` | 랜딩 웹뷰 |
| `screen-full-web` | 전체화면 웹뷰 |
| `easy-pay` | 간편결제 |
| `naver-pay` | 네이버페이 |
| `tmoney-pay` | 티머니페이 |
| `daum-post-code` | 다음 우편번호 |
| `verify-phone` | 휴대폰 인증 |
| `pass-web` | PASS 인증 웹뷰 |
| `chaevi-reward-web` | 채비 리워드 |
| `coupon-and-gift-web` | 쿠폰/선물 웹뷰 |
| `porsche` | 포르쉐 전용 |
| `porsche-qr` | 포르쉐 QR |

---

## 5. 이벤트(Event) 텍소노미

### 자동 수집 이벤트

SDK 레벨에서 자동 발생하므로 코드 작성 불필요.

| 이벤트 | 설명 |
|--------|------|
| `session_start` | 세션 시작 (비활동 30분 후 복귀 시 새 세션) |
| `session_end` | 세션 종료 (best-effort, `duration_ms` 포함) |
| `app_open` | 앱 실행 (SDK 초기화 시) |
| `screen_view` | 화면 전환 시 자동 발생 (`TrackScreenEvents` 필요) |
| `identify` | `Analytics.identify()` 호출 시 자동 전송 |

> `button_click`은 `AnalyticsButton` 컴포넌트를 사용해야 발생한다 (완전 자동은 아님).

### 수동 추적 이벤트

#### 인증 (Auth)

| 이벤트 | 속성 | 설명 | 상태 |
|--------|------|------|------|
| `login_success` | `method` (email) | 이메일 로그인 성공 | 수집 중 |
| `login_fail` | `method`, `error_code` | 이메일 로그인 실패 | 수집 중 |
| `social_login_success` | `social_type` | 소셜 로그인 성공 | 수집 중 |
| `social_login_fail` | `social_type`, `error_code` | 소셜 로그인 실패 | 수집 중 |
| `social_login_register` | `social_type` | 소셜 미가입자 회원가입 전환 | 수집 중 |
| `register_start` | `method` (email/social) | 회원가입 시작 | 신규 |
| `register_term_agree` | `method` | 약관 동의 완료 | 신규 |
| `register_verify` | `method` | 본인인증 완료 | 신규 |
| `register_complete` | `method` | 회원가입 완료 | 신규 |
| `logout` | - | 로그아웃 | 신규 |
| `withdrawal_complete` | - | 회원탈퇴 완료 | 신규 |
| `terms_revision_view` | `terms_type` | 약관개정 화면 진입 | 신규 |
| `terms_revision_agree` | `terms_type`, `is_agree` | 약관개정 동의/거부 | 신규 |

#### 충전 (Charging)

| 이벤트 | 속성 | 설명 | 상태 |
|--------|------|------|------|
| `charging_start` | `station_id`, `charger_id`, `method` (qr/pnc/nfc/remote) | 충전 시작 | 신규 |
| `charging_complete` | `station_id`, `duration_sec`, `kwh`, `amount` | 충전 완료 | 신규 |
| `charging_terminate` | `station_id`, `reason` | 충전 중단 | 신규 |
| `preoccupy_request` | `station_id`, `charger_id` | 선점 요청 | 신규 |
| `preoccupy_cancel` | `station_id`, `charger_id` | 선점 취소 | 신규 |

#### 충전소 탐색 (Station)

| 이벤트 | 속성 | 설명 | 상태 |
|--------|------|------|------|
| `station_search` | `keyword`, `result_count` | 충전소 검색 | 신규 |
| `station_detail_view` | `station_id` | 충전소 상세 조회 | 신규 |
| `station_favor_toggle` | `station_id`, `is_favor` | 즐겨찾기 추가/제거 | 신규 |
| `station_route_search` | - | 경로 탐색 | 신규 |
| `station_filter_apply` | `filter_values` | 필터 적용 | 신규 |

#### 멤버십 카드 (Card)

| 이벤트 | 속성 | 설명 | 상태 |
|--------|------|------|------|
| `card_register` | `card_type` (general/transit) | 카드 등록 | 신규 |
| `card_apply` | `card_type` | 실물카드 발급 신청 | 신규 |
| `card_lost_report` | `card_id` | 분실 신고 | 신규 |

#### 크레딧 (Credit)

| 이벤트 | 속성 | 설명 | 상태 |
|--------|------|------|------|
| `credit_charge` | `amount`, `payment_method` | 크레딧 충전 | 신규 |
| `credit_refund` | `amount` | 크레딧 환불 | 신규 |
| `credit_move` | `from_card`, `to_card`, `amount` | 크레딧 이동 | 신규 |
| `credit_auto_charge_set` | `amount`, `threshold` | 자동충전 설정 | 신규 |

#### 결제 (Payment)

| 이벤트 | 속성 | 설명 | 상태 |
|--------|------|------|------|
| `payment_add` | `payment_type` | 결제수단 등록 | 신규 |
| `payment_delete` | `payment_type` | 결제수단 삭제 | 신규 |
| `unpaid_pay` | `amount` | 미납 결제 | 신규 |

#### 구독 (Subscription)

| 이벤트 | 속성 | 설명 | 상태 |
|--------|------|------|------|
| `chaevi_pass_subscribe` | `pass_type` | 채비패스 가입 | 신규 |
| `chaevi_pass_cancel` | `pass_type` | 채비패스 해지 | 신규 |
| `chaevi_pass_change` | `from_type`, `to_type` | 채비패스 변경 | 신규 |
| `biz_pass_register` | `subscription_id` | 비즈패스 등록 | 신규 |
| `vmembers_cancel` | - | V멤버스 해지 | 신규 |

#### 사용자 행동 (User Action)

| 이벤트 | 속성 | 설명 | 상태 |
|--------|------|------|------|
| `button_click` | `button_id` | 주요 버튼 클릭 | 수집 중 |
| `banner_click` | `banner_id`, `position` (top/mid/bottom/popup) | 배너 클릭 | 신규 |
| `coupon_register` | `coupon_code` | 쿠폰 등록 | 신규 |
| `gift_send` | `amount` | 선물 전송 | 신규 |
| `point_transfer` | `partner`, `amount` | 포인트 전환 | 신규 |
| `inquiry_submit` | `category` | 1:1 문의 | 신규 |

#### 콘텐츠 (Content)

| 이벤트 | 속성 | 설명 | 상태 |
|--------|------|------|------|
| `push_open` | `push_type`, `push_id` | 푸시 열기 | 신규 |
| `notice_read` | `notice_id` | 알림 확인 | 신규 |
| `announcement_view` | `announcement_id` | 공지 조회 | 신규 |
| `event_view` | `event_id` | 이벤트 조회 | 신규 |

#### 차량 (Vehicle)

| 이벤트 | 속성 | 설명 | 상태 |
|--------|------|------|------|
| `vehicle_register` | `brand`, `model` | 차량 등록 | 신규 |
| `vehicle_edit` | `vehicle_id` | 차량 수정 | 신규 |
| `vehicle_delete` | `vehicle_id` | 차량 삭제 | 신규 |
| `my_car_connect` | `vehicle_id` | 마이카 연동 | 신규 |

---

## 6. 사용자 속성 (User Properties)

> **현재 미구현.** SDK에 `Analytics.setUserProperty()` API가 없다. 현재는 `Analytics.identify(memberId)` 로 회원 ID 연결만 가능하다. 아래 속성은 향후 SDK API 추가 시 구현 예정.

| 속성 | 타입 | 설명 |
|------|------|------|
| `member_id` | String | 회원 ID (`identify()`로 구현됨) |
| `login_method` | String | 마지막 로그인 방식 |
| `membership_type` | String | 멤버십 유형 |
| `has_card` | Boolean | 카드 보유 여부 |
| `vehicle_count` | Int | 등록 차량 수 |
| `pass_type` | String? | 채비패스/비즈패스 유형 |

---

## 7. 적용 우선순위

| 순위 | 범위 | 이유 |
|------|------|------|
| P0 | 충전 퍼널 (`charging_*`) | 앱의 핵심 비즈니스인데 추적 0건 |
| P1 | 회원가입 퍼널 (`register_*`) | 로그인은 추적하지만 가입 단계별 이탈 파악 불가 |
| P2 | 멤버십/결제 (`card_*`, `credit_*`, `payment_*`) | 매출 직결 |
| P3 | 충전소 탐색 (`station_*`) | 사용자 행동 이해 |
| P4 | 콘텐츠 (`push_open`, `banner_click` 등) | 마케팅 효과 측정 |
| P5 | 차량/기타 | 부가 기능 |

---

## 8. 코드 적용 가이드

### 이벤트 추적

```kotlin
// ViewModel에서
Analytics.track("charging_start", mapOf(
    "station_id" to stationId,
    "charger_id" to chargerId,
    "method" to "qr"
))
```

### 버튼 클릭 (View에서)

```kotlin
// AnalyticsButton 사용 — button_id에 {화면}_{액션} 패턴으로 이름 짓기
AnalyticsButton(
    buttonId = "membership_charge_credit_submit",
    onClick = { onSubmit() }
) {
    Text("충전하기")
}
```

> `button_id`를 `{화면}_{액션}` 패턴으로 지으면 이름만으로 어느 화면의 어떤 버튼인지 구분할 수 있다.

### 사용자 식별 (로그인 성공 시)

```kotlin
Analytics.identify(memberId.toString())
```

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-10-26 | 초안 작성. AppRoute 144개 기준 전체 화면 매핑, 이벤트 분류 |
| 2026-10-26 | 수정. screen_id를 route 경로 그대로 사용하도록 변경 (파라미터 자동 제거). 자동 수집 이벤트 목록 보완 (`app_open`, `identify` 추가). 세션 타임아웃 1분→30분 수정. 사용자 속성 미구현 상태 명시. 플랫폼별 속성 값 정책 추가 |
