package com.example.testdemo.feature.lab

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.testdemo.lib.base.compose.MviScreen
import com.example.testdemo.lib.base.compose.UiStateHost
import com.example.testdemo.lib.base.ui.AppTitleBar
import com.example.testdemo.lib.image.AppImage
import com.example.testdemo.lib.image.ImageLoader
import com.example.testdemo.lib.image.ImageOptions
import com.example.testdemo.lib.log.AppLogger
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private const val DEMO_IMAGE = "https://picsum.photos/seed/scaffold/800/400"

/**
 * 「能力演示」页：集中演示 lib 里不易在主流程体现的能力。
 */
@Composable
fun LabScreen(
    viewModel: LabViewModel,
    onBack: () -> Unit,
    onOpenParamRoute: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    MviScreen(
        viewModel = viewModel,
        onEvent = { event ->
            when (event) {
                is LabEvent.ShowMessage -> AppLogger.i("Lab", event.message)
            }
        },
    ) { state ->
        Column(Modifier.fillMaxSize()) {
            AppTitleBar(title = "能力演示", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
            Text(
                "集中演示 lib 中不易在主流程体现的能力",
                style = MaterialTheme.typography.bodySmall,
            )

            // ── 1. UiState 四态 ──
            SectionTitle("1. MVI / UiState 四态（lib:base）")
            ScrollableRow {
                OutlinedButton({ viewModel.dispatch(LabIntent.ShowIdle) }) { Text("Idle") }
                OutlinedButton({ viewModel.dispatch(LabIntent.ShowLoading) }) { Text("Loading") }
                OutlinedButton({ viewModel.dispatch(LabIntent.ShowSuccess) }) { Text("Success") }
                OutlinedButton({ viewModel.dispatch(LabIntent.ShowEmpty) }) { Text("Empty") }
                OutlinedButton({ viewModel.dispatch(LabIntent.ShowError) }) { Text("Error") }
            }
            Box(Modifier.fillMaxWidth().height(130.dp)) {
                UiStateHost(
                    state = state.uiState,
                    onRetry = { viewModel.dispatch(LabIntent.ShowSuccess) },
                ) { text -> Text(text, Modifier.padding(16.dp)) }
            }

            // ── 2. 日志 ──
            SectionTitle("2. 日志 AppLogger（lib:log）")
            ScrollableRow {
                listOf("d", "i", "w", "e").forEach { level ->
                    OutlinedButton({ viewModel.dispatch(LabIntent.WriteLog(level)) }) { Text(level) }
                }
            }
            Text("最近一条：${state.lastLog}", style = MaterialTheme.typography.bodySmall)

            // ── 3. 崩溃兜底 ──
            SectionTitle("3. 崩溃兜底 CrashGuard（lib:log）")
            Button(
                onClick = { viewModel.dispatch(LabIntent.Crash) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text("触发测试崩溃（App 会闪退）") }
            Text(
                "CrashGuard 会记日志并转交系统默认处理，同时留出上报入口",
                style = MaterialTheme.typography.bodySmall,
            )

            // ── 4. 扫描防抖 ──
            SectionTitle("4. 扫描枪防抖 EnterKeyDebouncer（lib:base）")
            Button({ viewModel.dispatch(LabIntent.SimulateScanBurst) }) { Text("模拟连击 5 次") }
            Text(
                "接受 ${state.scanAccepted} 次 / 拦截 ${state.scanBlocked} 次",
                style = MaterialTheme.typography.bodySmall,
            )

            // ── 5. 图片 ──
            SectionTitle("5. 图片能力 AppImage（lib:image）")
            AppImage(
                model = DEMO_IMAGE,
                modifier = Modifier.fillMaxWidth().height(140.dp),
                options = ImageOptions.builder().diskCache(true).build(),
                contentScale = ContentScale.Crop,
            )
            ScrollableRow {
                OutlinedButton({ scope.launch { ImageLoader.preload(context, DEMO_IMAGE) } }) { Text("预加载") }
                OutlinedButton({ ImageLoader.clearMemory(context) }) { Text("清内存缓存") }
                OutlinedButton({ ImageLoader.clearDisk(context) }) { Text("清磁盘缓存") }
            }

            // ── 6. 错误映射 ──
            SectionTitle("6. 网络错误映射 ErrorMapper（lib:net）")
            ScrollableRow {
                listOf(
                    "timeout" to "超时",
                    "parse" to "解析",
                    "connect" to "连接",
                    "http" to "HTTP",
                    "business" to "业务",
                ).forEach { (kind, label) ->
                    OutlinedButton({ viewModel.dispatch(LabIntent.MapError(kind)) }) { Text(label) }
                }
            }
            Text(state.errorMapping, style = MaterialTheme.typography.bodySmall)

            // ── 7. 带参数路由 ──
            SectionTitle("7. 带参数路由 AppRoute（lib:router）")
            Button({ onOpenParamRoute("42") }) { Text("跳转到 AppRoute.Detail(\"42\")") }
            Text(
                "路由表在 AppRoute，栈管理在 AppRouter（navigate/back/replace/popTo/resetTo）",
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(10.dp))
    Text(text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun ScrollableRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}
