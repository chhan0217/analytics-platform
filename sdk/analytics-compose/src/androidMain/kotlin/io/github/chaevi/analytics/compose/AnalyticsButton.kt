package io.github.chaevi.analytics.compose

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.chaevi.analytics.sdk.api.Analytics

/**
 * 클릭 시 자동으로 button_click 이벤트를 추적하는 Button 래퍼.
 */
@Composable
fun AnalyticsButton(
    buttonId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    properties: Map<String, Any?> = emptyMap(),
    content: @Composable () -> Unit,
) {
    Button(
        onClick = {
            val merged = mutableMapOf<String, Any?>("button_id" to buttonId)
            merged.putAll(properties)
            Analytics.track("button_click", merged)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
    ) {
        content()
    }
}
