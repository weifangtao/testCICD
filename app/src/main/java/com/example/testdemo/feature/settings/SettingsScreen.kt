package com.example.testdemo.feature.settings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.testdemo.lib.config.AppEnvironment
import com.example.testdemo.lib.base.ui.AppTitleBar
import com.example.testdemo.lib.config.AppEnvironmentConfigHolder
import com.example.testdemo.lib.log.AppLogger
import com.example.testdemo.AppApplication
import com.example.testdemo.BuildConfig
import kotlinx.coroutines.launch

private const val KEY_LAST_CHECK = "last_check"

/** 演示环境切换 + 存储读写。 */
@Composable
fun SettingsScreen(
    app: AppApplication,
    onBack: () -> Unit,
    onOpenLab: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val storage = remember(app.scopeKey) { app.storage }
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val environment by AppEnvironmentConfigHolder.environment.collectAsStateWithLifecycle()
    val sessionState by app.sessions.state.collectAsStateWithLifecycle()
    var storedValue by remember { mutableStateOf("读取中…") }

    LaunchedEffect(storage) {
        // lib:storage —— 订阅式读取：写入后自动刷新，不用手动再查一次
        storage.observeString(KEY_LAST_CHECK).collect { storedValue = it ?: "未写入" }
    }

    Column(Modifier.fillMaxSize()) {
        // 统一标题栏：左返回
        AppTitleBar(title = "设置", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

        Text("当前环境：${environment.key}", style = MaterialTheme.typography.titleMedium)
        Text("Base URL：${environment.baseUrl}", style = MaterialTheme.typography.bodySmall)
        Text(
            "HTTP 日志：${if (BuildConfig.DEBUG && environment.enableHttpLogs) "开" else "关"}",
            style = MaterialTheme.typography.bodySmall,
        )

        Text("切换环境后需重新登录", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppEnvironment.entries.forEach { target ->
                OutlinedButton(
                    onClick = {
                        busy = true
                        scope.launch {
                            try { app.sessions.switchEnvironment(target) }
                            catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (_: Exception) { actionError = "切换失败，请重试" }
                            finally { busy = false }
                        }
                    },
                    enabled = BuildConfig.DEBUG && !busy && target != environment,
                ) { Text(target.key) }
            }
        }

        Text("账号：${sessionState.session?.user?.userCode.orEmpty()}")
        sessionState.session?.user?.houseName?.let { Text("仓库：$it") }
        OutlinedButton(onClick = {
            busy = true
            scope.launch {
                try { app.sessions.logout() }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { actionError = "退出失败，请重试" }
                finally { busy = false }
            }
        }, enabled = !busy) { Text("退出登录") }
        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Text("存储值：$storedValue", style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = {
                val now = System.currentTimeMillis().toString()
                scope.launch {
                    storage.putString(KEY_LAST_CHECK, now)
                    storedValue = now
                    AppLogger.i("Settings", "storage write: $now")
                }
            }
        ) { Text("写入存储") }

        Spacer(Modifier.height(8.dp))

        Text("能力演示", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = onOpenLab) { Text("打开能力演示页（UiState / 日志 / 崩溃 / 防抖 / 图片 / 错误映射）") }

        }
    }
}
