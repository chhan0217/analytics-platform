package io.github.chaevi.analytics.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.chaevi.analytics.compose.AnalyticsButton
import io.github.chaevi.analytics.compose.TrackScreenEvents
import io.github.chaevi.analytics.sdk.api.Analytics
import io.github.chaevi.analytics.sdk.api.AnalyticsConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        io.github.chaevi.analytics.sdk.data.local
            .initAndroidStorage(applicationContext)
        Analytics.initialize(
            AnalyticsConfig(
                apiKey = "ink_b4e484381a6e5818000fab87f7b0f67beda26a0fc128ddf8749962c0ce3ab4f4",
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
                    SampleApp()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Analytics.onBackground()
    }

    override fun onStart() {
        super.onStart()
        if (Analytics.isInitialized()) {
            Analytics.onForeground()
        }
    }
}

@Composable
fun SampleApp() {
    val navController = rememberNavController()
    TrackScreenEvents(navController)

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("product_list") { ProductListScreen(navController) }
        composable("product_detail/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: "unknown"
            ProductDetailScreen(navController, productId)
        }
    }
}

data class LogEntry(
    val time: String,
    val action: String,
    val detail: String,
)

@Composable
fun HomeScreen(navController: NavController) {
    var loggedIn by remember { mutableStateOf(false) }
    var eventCount by remember { mutableStateOf(0) }
    val logs = remember { mutableStateListOf<LogEntry>() }

    fun addLog(
        action: String,
        detail: String,
    ) {
        val time =
            java.text
                .SimpleDateFormat(
                    "HH:mm:ss.SSS",
                    java.util.Locale.getDefault(),
                ).format(java.util.Date())
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
                buttonId = "go_product_list",
                onClick = {
                    navController.navigate("product_list")
                    addLog("navigate", "product_list")
                },
            ) {
                Text("상품 목록 →")
            }

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

private val sampleProducts =
    listOf(
        "p-001" to "무선 키보드",
        "p-002" to "블루투스 마우스",
        "p-003" to "USB-C 허브",
        "p-004" to "모니터 암",
        "p-005" to "노트북 스탠드",
    )

@Composable
fun ProductListScreen(navController: NavController) {
    Column(
        modifier =
            Modifier
                .padding(16.dp)
                .fillMaxSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text("← 홈")
            }
            Text(
                text = "상품 목록",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sampleProducts) { (id, name) ->
                Card(
                    onClick = {
                        Analytics.track("product_view", mapOf("product_id" to id))
                        navController.navigate("product_detail/$id")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(text = name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = id,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        Text("→", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: String,
) {
    val productName = sampleProducts.find { it.first == productId }?.second ?: "알 수 없는 상품"

    Column(
        modifier =
            Modifier
                .padding(16.dp)
                .fillMaxSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text("← 목록")
            }
            Text(
                text = "상품 상세",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = productName, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "상품 ID: $productId",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnalyticsButton(
            buttonId = "add_to_cart",
            onClick = {
                Analytics.track(
                    "add_to_cart",
                    mapOf("product_id" to productId, "product_name" to productName),
                )
            },
        ) {
            Text("장바구니 담기")
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnalyticsButton(
            buttonId = "purchase",
            onClick = {
                Analytics.track(
                    "purchase",
                    mapOf("product_id" to productId, "amount" to "29900"),
                )
            },
        ) {
            Text("구매하기")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = {
            Analytics.flush()
        }) {
            Text("즉시 전송 (flush)")
        }
    }
}
