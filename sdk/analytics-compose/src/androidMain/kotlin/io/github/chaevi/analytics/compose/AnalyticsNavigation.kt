package io.github.chaevi.analytics.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import io.github.chaevi.analytics.sdk.api.Analytics

/**
 * NavController의 화면 전환을 감지하여 자동으로 screen_view 이벤트를 추적한다.
 */
@Composable
fun TrackScreenEvents(navController: NavController) {
    DisposableEffect(navController) {
        val listener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                val route = destination.route ?: return@OnDestinationChangedListener
                Analytics.trackScreen(route)
            }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}
