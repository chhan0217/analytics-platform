package io.github.chaevi.analytics.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chaevi.analytics.compose.AnalyticsButton
import io.github.chaevi.analytics.sdk.api.Analytics
import io.github.chaevi.analytics.sdk.api.AnalyticsConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        io.github.chaevi.analytics.sdk.data.local
            .initAndroidStorage(applicationContext)
        Analytics.initialize(
            AnalyticsConfig(
                apiKey = "ink_5992c0cec7840537bb6336f9419a4821b4a34e21ec450e274e3ef275f30ec8b4",
                baseUrl = "http://172.168.10.59:8080",
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

data class LogEntry(
    val time: String,
    val action: String,
    val detail: String,
)

@Composable
fun SampleScreen() {
    var loggedIn by remember { mutableStateOf(false) }
    var eventCount by remember { mutableStateOf(0) }
    val logs = remember { mutableStateListOf<LogEntry>() }

    fun addLog(
        action: String,
        detail: String,
    ) {
        val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        logs.add(0, LogEntry(time, action, detail))
    }

    Column(
        modifier =
            Modifier
                .padding(16.dp)
                .fillMaxSize(),
    ) {
        Text(
            text = "Analytics Sample App",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (loggedIn) "로그인: user_1024" else "비로그인",
            style = MaterialTheme.typography.bodySmall,
            color = if (loggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnalyticsButton(
                buttonId = "track_custom",
                onClick = {
                    val id = "p-${++eventCount}"
                    Analytics.track("product_view", mapOf("product_id" to id))
                    addLog("track", "product_view {product_id: $id}")
                },
            ) {
                Text("커스텀 이벤트 전송")
            }

            AnalyticsButton(
                buttonId = "screen_view",
                onClick = {
                    Analytics.trackScreen("sample_screen")
                    addLog("trackScreen", "screen_view {screen_id: sample_screen}")
                },
            ) {
                Text("화면 조회 이벤트")
            }

            if (!loggedIn) {
                OutlinedButton(onClick = {
                    Analytics.identify("user_1024")
                    loggedIn = true
                    addLog("identify", "user_1024")
                }) {
                    Text("로그인 (identify)")
                }
            } else {
                OutlinedButton(onClick = {
                    Analytics.resetIdentity()
                    loggedIn = false
                    addLog("resetIdentity", "로그아웃")
                }) {
                    Text("로그아웃 (resetIdentity)")
                }
            }

            OutlinedButton(onClick = {
                Analytics.flush()
                addLog("flush", "큐 즉시 전송 요청")
            }) {
                Text("즉시 전송 (flush)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "API 요청 로그 (${logs.size}건)",
            style = MaterialTheme.typography.titleSmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "버튼을 눌러 이벤트를 전송하세요",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 13.sp,
                )
            }
            logs.forEach { log ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                ) {
                    Text(
                        text = "${log.time}  ${log.action}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = log.detail,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
