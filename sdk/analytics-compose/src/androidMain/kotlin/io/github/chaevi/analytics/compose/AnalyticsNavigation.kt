package io.github.chaevi.analytics.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController

/**
 * NavController의 화면 전환을 감지하여 자동으로 screen_view 이벤트를 추적한다.
 * Phase 3에서 구현 예정.
 */
@Composable
fun TrackScreenEvents(navController: NavController) {
    DisposableEffect(navController) {
        val listener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                val route = destination.route ?: return@OnDestinationChangedListener
                // TODO: Analytics.trackScreen(route) 호출 (Phase 3)
            }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}
