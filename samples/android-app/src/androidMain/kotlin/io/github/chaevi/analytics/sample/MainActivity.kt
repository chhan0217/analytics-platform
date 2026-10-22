package io.github.chaevi.analytics.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.chaevi.analytics.compose.AnalyticsButton
import io.github.chaevi.analytics.sdk.api.Analytics
import io.github.chaevi.analytics.sdk.api.AnalyticsConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Analytics.initialize(
            AnalyticsConfig(
                apiKey = "REPLACE_WITH_INGESTION_KEY",
                baseUrl = "http://10.0.2.2:8080",
                environment = "development",
                appVersion = "0.1.0",
                enableLogging = true,
                errorListener = { error ->
                    Log.e("Analytics", "에러: ${error.code} - ${error.message}")
                },
            ),
        )

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SampleScreen()
                }
            }
        }
    }
}

@Composable
fun SampleScreen() {
    var loggedIn by remember { mutableStateOf(false) }
    var eventCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Analytics Sample App",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnalyticsButton(
            buttonId = "track_custom",
            onClick = {
                Analytics.track("product_view", mapOf("product_id" to "p-${++eventCount}"))
            },
        ) {
            Text("커스텀 이벤트 전송")
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnalyticsButton(
            buttonId = "screen_view",
            onClick = {
                Analytics.trackScreen("sample_screen")
            },
        ) {
            Text("화면 조회 이벤트")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!loggedIn) {
            OutlinedButton(onClick = {
                Analytics.identify("user_1024")
                loggedIn = true
            }) {
                Text("로그인 (identify)")
            }
        } else {
            OutlinedButton(onClick = {
                Analytics.resetIdentity()
                loggedIn = false
            }) {
                Text("로그아웃 (resetIdentity)")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = {
            Analytics.flush()
        }) {
            Text("즉시 전송 (flush)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (loggedIn) "로그인 상태: user_1024" else "비로그인 상태",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
