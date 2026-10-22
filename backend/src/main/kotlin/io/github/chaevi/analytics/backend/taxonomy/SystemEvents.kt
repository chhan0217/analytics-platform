package io.github.chaevi.analytics.backend.taxonomy

data class SystemEventDef(
    val eventKey: String,
    val displayName: String,
    val description: String,
)

val SYSTEM_EVENTS =
    listOf(
        SystemEventDef("session_start", "세션 시작", "세션이 시작될 때 자동 전송"),
        SystemEventDef("session_end", "세션 종료", "세션이 종료될 때 전송 (best-effort)"),
        SystemEventDef("screen_view", "화면 조회", "화면이 표시될 때 전송"),
        SystemEventDef("app_open", "앱 실행", "앱이 실행될 때 전송"),
        SystemEventDef("button_click", "버튼 클릭", "버튼 클릭 시 전송"),
        SystemEventDef("identify", "사용자 식별", "로그인 사용자 식별 시 전송"),
    )
