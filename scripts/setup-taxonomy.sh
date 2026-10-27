#!/bin/bash
# 텍소노미 + 앱 + 수집키 전체 셋업 스크립트
# 사용법: ./scripts/setup-taxonomy.sh

set -e
BASE="http://localhost:8080"

echo "=== 1. 로그인 ==="
TOKEN=$(curl -s $BASE/admin/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@analytics.local","password":"admin1234"}' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['token'])")

echo "=== 2. 조직/프로젝트 확인 ==="
ORG_ID=$(curl -s "$BASE/admin/organizations" -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['data'][0]['id'])")
PROJECT=$(curl -s "$BASE/admin/organizations/$ORG_ID/projects" -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['data'][0]['id'])")
echo "Project: $PROJECT"

echo "=== 3. 앱 + 수집키 생성 ==="
APP_ID=$(curl -s -X POST "$BASE/admin/projects/$PROJECT/applications" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"chaevi","platform":"ANDROID"}' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['id'])")

INGESTION_KEY=$(curl -s -X POST "$BASE/admin/projects/$PROJECT/applications/$APP_ID/credentials" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['ingestion_key'])")
echo "수집키: $INGESTION_KEY"

echo "=== 4. 시스템 이벤트 속성 등록 ==="
get_id() {
  curl -s "$BASE/admin/projects/$PROJECT/taxonomy/events?page_size=100" \
    -H "Authorization: Bearer $TOKEN" \
    | python3 -c "import json,sys; [print(e['id']) for e in json.load(sys.stdin)['data'] if e['event_key']=='$1']"
}
ap() {
  curl -s -X POST "$BASE/admin/projects/$PROJECT/taxonomy/events/$1/properties" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"property_key\":\"$2\",\"value_type\":\"$3\",\"required\":$4}" > /dev/null
}
re() {
  curl -s -X POST "$BASE/admin/projects/$PROJECT/taxonomy/events" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"event_key\":\"$1\",\"display_name\":\"$2\",\"description\":\"$3\"}" \
    | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('data',{}).get('id',''))" 2>/dev/null
}

ID=$(get_id "screen_view"); ap "$ID" "screen_id" "STRING" "true"
ID=$(get_id "button_click"); ap "$ID" "button_id" "STRING" "true"
ID=$(get_id "session_end"); ap "$ID" "duration_ms" "INTEGER" "false"
ID=$(get_id "identify"); ap "$ID" "external_user_id" "STRING" "true"

echo "=== 5. 커스텀 이벤트 등록 ==="
# 인증
ID=$(re "login_success" "로그인 성공" "이메일 로그인 성공"); [ -n "$ID" ] && ap "$ID" "method" "STRING" "true"
ID=$(re "login_fail" "로그인 실패" "이메일 로그인 실패"); [ -n "$ID" ] && { ap "$ID" "method" "STRING" "true"; ap "$ID" "error_code" "STRING" "false"; }
ID=$(re "social_login_success" "소셜 로그인 성공" "소셜 로그인 성공"); [ -n "$ID" ] && ap "$ID" "social_type" "STRING" "true"
ID=$(re "social_login_fail" "소셜 로그인 실패" "소셜 로그인 실패"); [ -n "$ID" ] && { ap "$ID" "social_type" "STRING" "true"; ap "$ID" "error_code" "STRING" "false"; }
ID=$(re "social_login_register" "소셜 회원가입 전환" "소셜 미가입자 회원가입 전환"); [ -n "$ID" ] && ap "$ID" "social_type" "STRING" "true"
ID=$(re "register_start" "회원가입 시작" "회원가입 시작"); [ -n "$ID" ] && ap "$ID" "method" "STRING" "true"
ID=$(re "register_term_agree" "약관 동의 완료" "약관 동의 완료"); [ -n "$ID" ] && ap "$ID" "method" "STRING" "true"
ID=$(re "register_verify" "본인인증 완료" "본인인증 완료"); [ -n "$ID" ] && ap "$ID" "method" "STRING" "true"
ID=$(re "register_complete" "회원가입 완료" "회원가입 완료"); [ -n "$ID" ] && ap "$ID" "method" "STRING" "true"
re "logout" "로그아웃" "로그아웃" > /dev/null
re "withdrawal_complete" "회원탈퇴 완료" "회원탈퇴 완료" > /dev/null
ID=$(re "terms_revision_view" "약관개정 화면 진입" "약관개정 화면 진입"); [ -n "$ID" ] && ap "$ID" "terms_type" "STRING" "true"
ID=$(re "terms_revision_agree" "약관개정 동의" "약관개정 동의/거부"); [ -n "$ID" ] && { ap "$ID" "terms_type" "STRING" "true"; ap "$ID" "is_agree" "BOOLEAN" "true"; }

# 충전
ID=$(re "charging_start" "충전 시작" "충전 시작"); [ -n "$ID" ] && { ap "$ID" "station_id" "STRING" "true"; ap "$ID" "charger_id" "STRING" "true"; ap "$ID" "method" "STRING" "true"; }
ID=$(re "charging_complete" "충전 완료" "충전 완료"); [ -n "$ID" ] && { ap "$ID" "station_id" "STRING" "true"; ap "$ID" "duration_sec" "INTEGER" "true"; ap "$ID" "kwh" "FLOAT" "true"; ap "$ID" "amount" "INTEGER" "true"; }
ID=$(re "charging_terminate" "충전 중단" "충전 중단"); [ -n "$ID" ] && { ap "$ID" "station_id" "STRING" "true"; ap "$ID" "reason" "STRING" "true"; }
ID=$(re "preoccupy_request" "선점 요청" "선점 요청"); [ -n "$ID" ] && { ap "$ID" "station_id" "STRING" "true"; ap "$ID" "charger_id" "STRING" "true"; }
ID=$(re "preoccupy_cancel" "선점 취소" "선점 취소"); [ -n "$ID" ] && { ap "$ID" "station_id" "STRING" "true"; ap "$ID" "charger_id" "STRING" "true"; }

# 충전소
ID=$(re "station_search" "충전소 검색" "충전소 검색"); [ -n "$ID" ] && { ap "$ID" "keyword" "STRING" "false"; ap "$ID" "result_count" "INTEGER" "false"; }
ID=$(re "station_detail_view" "충전소 상세 조회" "충전소 상세 조회"); [ -n "$ID" ] && ap "$ID" "station_id" "STRING" "true"
ID=$(re "station_favor_toggle" "충전소 즐겨찾기" "즐겨찾기"); [ -n "$ID" ] && { ap "$ID" "station_id" "STRING" "true"; ap "$ID" "is_favor" "BOOLEAN" "true"; }
re "station_route_search" "경로 탐색" "경로 탐색" > /dev/null
ID=$(re "station_filter_apply" "필터 적용" "필터 적용"); [ -n "$ID" ] && ap "$ID" "filter_values" "STRING" "false"

# 카드
ID=$(re "card_register" "카드 등록" "카드 등록"); [ -n "$ID" ] && ap "$ID" "card_type" "STRING" "true"
ID=$(re "card_apply" "카드 발급 신청" "카드 발급 신청"); [ -n "$ID" ] && ap "$ID" "card_type" "STRING" "true"
ID=$(re "card_lost_report" "카드 분실 신고" "카드 분실 신고"); [ -n "$ID" ] && ap "$ID" "card_id" "STRING" "true"

# 크레딧
ID=$(re "credit_charge" "크레딧 충전" "크레딧 충전"); [ -n "$ID" ] && { ap "$ID" "amount" "INTEGER" "true"; ap "$ID" "payment_method" "STRING" "true"; }
ID=$(re "credit_refund" "크레딧 환불" "크레딧 환불"); [ -n "$ID" ] && ap "$ID" "amount" "INTEGER" "true"
ID=$(re "credit_move" "크레딧 이동" "크레딧 이동"); [ -n "$ID" ] && { ap "$ID" "from_card" "STRING" "true"; ap "$ID" "to_card" "STRING" "true"; ap "$ID" "amount" "INTEGER" "true"; }
ID=$(re "credit_auto_charge_set" "자동충전 설정" "자동충전 설정"); [ -n "$ID" ] && { ap "$ID" "amount" "INTEGER" "true"; ap "$ID" "threshold" "INTEGER" "true"; }

# 결제
ID=$(re "payment_add" "결제수단 등록" "결제수단 등록"); [ -n "$ID" ] && ap "$ID" "payment_type" "STRING" "true"
ID=$(re "payment_delete" "결제수단 삭제" "결제수단 삭제"); [ -n "$ID" ] && ap "$ID" "payment_type" "STRING" "true"
ID=$(re "unpaid_pay" "미납 결제" "미납 결제"); [ -n "$ID" ] && ap "$ID" "amount" "INTEGER" "true"

# 구독
ID=$(re "chaevi_pass_subscribe" "채비패스 가입" "채비패스 가입"); [ -n "$ID" ] && ap "$ID" "pass_type" "STRING" "true"
ID=$(re "chaevi_pass_cancel" "채비패스 해지" "채비패스 해지"); [ -n "$ID" ] && ap "$ID" "pass_type" "STRING" "true"
ID=$(re "chaevi_pass_change" "채비패스 변경" "채비패스 변경"); [ -n "$ID" ] && { ap "$ID" "from_type" "STRING" "true"; ap "$ID" "to_type" "STRING" "true"; }
ID=$(re "biz_pass_register" "비즈패스 등록" "비즈패스 등록"); [ -n "$ID" ] && ap "$ID" "subscription_id" "STRING" "true"
re "vmembers_cancel" "V멤버스 해지" "V멤버스 해지" > /dev/null

# 사용자 행동
ID=$(re "banner_click" "배너 클릭" "배너 클릭"); [ -n "$ID" ] && { ap "$ID" "banner_id" "STRING" "true"; ap "$ID" "position" "STRING" "false"; }
ID=$(re "coupon_register" "쿠폰 등록" "쿠폰 등록"); [ -n "$ID" ] && ap "$ID" "coupon_code" "STRING" "true"
ID=$(re "gift_send" "선물 전송" "선물 전송"); [ -n "$ID" ] && ap "$ID" "amount" "INTEGER" "true"
ID=$(re "point_transfer" "포인트 전환" "포인트 전환"); [ -n "$ID" ] && { ap "$ID" "partner" "STRING" "true"; ap "$ID" "amount" "INTEGER" "true"; }
ID=$(re "inquiry_submit" "1:1 문의" "1:1 문의"); [ -n "$ID" ] && ap "$ID" "category" "STRING" "false"

# 콘텐츠
ID=$(re "push_open" "푸시 열기" "푸시 열기"); [ -n "$ID" ] && { ap "$ID" "push_type" "STRING" "false"; ap "$ID" "push_id" "STRING" "false"; }
ID=$(re "notice_read" "알림 확인" "알림 확인"); [ -n "$ID" ] && ap "$ID" "notice_id" "STRING" "true"
ID=$(re "announcement_view" "공지 조회" "공지 조회"); [ -n "$ID" ] && ap "$ID" "announcement_id" "STRING" "true"
ID=$(re "event_view" "이벤트 조회" "이벤트 조회"); [ -n "$ID" ] && ap "$ID" "event_id" "STRING" "true"

# 차량
ID=$(re "vehicle_register" "차량 등록" "차량 등록"); [ -n "$ID" ] && { ap "$ID" "brand" "STRING" "false"; ap "$ID" "model" "STRING" "false"; }
ID=$(re "vehicle_edit" "차량 수정" "차량 수정"); [ -n "$ID" ] && ap "$ID" "vehicle_id" "STRING" "true"
ID=$(re "vehicle_delete" "차량 삭제" "차량 삭제"); [ -n "$ID" ] && ap "$ID" "vehicle_id" "STRING" "true"
ID=$(re "my_car_connect" "마이카 연동" "마이카 연동"); [ -n "$ID" ] && ap "$ID" "vehicle_id" "STRING" "true"

# 화면 체류시간
ID=$(re "screen_leave" "화면 이탈" "화면을 떠날 때 체류시간과 함께 전송"); [ -n "$ID" ] && { ap "$ID" "screen_id" "STRING" "true"; ap "$ID" "duration_ms" "INTEGER" "true"; }

echo ""
echo "=== 완료 ==="
echo "수집키: $INGESTION_KEY"
echo "프로젝트: $PROJECT"
echo ""
echo "채비 앱 SDK 설정:"
echo "  apiKey = \"$INGESTION_KEY\""
echo "  baseUrl = \"http://$(ifconfig | grep 'inet ' | grep -v 127.0.0.1 | awk '{print $2}'):8080\""
