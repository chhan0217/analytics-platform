package io.github.chaevi.analytics.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import io.github.chaevi.analytics.sdk.api.Analytics

/**
 * NavController의 화면 전환을 감지하여 자동으로 screen_view / screen_leave 이벤트를 추적한다.
 * - 화면 진입 시: screen_view (screen_id)
 * - 화면 이탈 시: screen_leave (screen_id, duration_ms)
 * route에서 쿼리 파라미터(?key=value)와 경로 파라미터(/{param})를 제거하여
 * 깨끗한 screen_id를 전송한다.
 */
@Composable
fun TrackScreenEvents(navController: NavController) {
    DisposableEffect(navController) {
        var currentScreenId: String? = null
        var screenEnteredAt: Long = 0L

        val listener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                val route = destination.route ?: return@OnDestinationChangedListener
                val screenId = stripRouteParameters(route)
                val now = System.currentTimeMillis()

                // 이전 화면 screen_leave 전송
                val prevScreen = currentScreenId
                if (prevScreen != null && prevScreen != screenId) {
                    val durationMs = now - screenEnteredAt
                    Analytics.trackScreenLeave(prevScreen, durationMs)
                }

                // 새 화면 screen_view 전송
                currentScreenId = screenId
                screenEnteredAt = now
                Analytics.trackScreen(screenId)
            }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            // 마지막 화면 screen_leave 전송
            val prevScreen = currentScreenId
            if (prevScreen != null) {
                val durationMs = System.currentTimeMillis() - screenEnteredAt
                Analytics.trackScreenLeave(prevScreen, durationMs)
            }
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}

/**
 * route에서 파라미터를 제거한다.
 * - "login?email={email}&isForPorsche={isForPorsche}" → "login"
 * - "main/my-page/announcement/detail/{announcementItem}" → "main/my-page/announcement/detail"
 * - "main/membership/auth-user/{cardUid}/{upstream}?credit={credit}" → "main/membership/auth-user"
 */
internal fun stripRouteParameters(route: String): String =
    route
        .substringBefore("?")
        .replace(Regex("/\\{[^}]+\\}"), "")
        .trimEnd('/')
