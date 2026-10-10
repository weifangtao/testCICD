package com.example.testdemo.lib.base.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.testdemo.lib.base.mvi.MviIntent
import com.example.testdemo.lib.base.mvi.MviSingleEvent
import com.example.testdemo.lib.base.mvi.MviView
import com.example.testdemo.lib.base.mvi.MviViewModel
import com.example.testdemo.lib.base.mvi.MviViewState
import com.example.testdemo.lib.log.AppLogger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * MVI Activity 基类（Compose 版）。
 *
 * 基类自动完成四件事：
 * 1. **统一标题栏**（左返回 / 中标题 / 右操作区），见 [titleBarConfig]
 * 2. `viewState` → [render]
 * 3. `singleEvent` → [handleSingleEvent]
 * 4. [viewIntents] → `vm.processIntent`
 *
 * 子类只需要声明 [vm] 并实现 [render]；标题与右侧操作按需覆写：
 *
 * ```
 * class DetailActivity : AbstractMviActivity<DetailIntent, DetailState, DetailEvent, DetailViewModel>() {
 *
 *     override val vm: DetailViewModel by viewModels { ... }
 *
 *     // 中间标题：可由状态推导
 *     override fun titleBarConfig(state: DetailState) = TitleBarConfig(title = "详情 #${state.id}")
 *
 *     // 右侧操作区：文字 / 图片 / 自定义
 *     @Composable
 *     override fun TitleBarActions(state: DetailState) {
 *         TitleBarTextAction(if (state.favorite) "已收藏" else "收藏") {
 *             sendIntent(DetailIntent.ToggleFavorite)
 *         }
 *     }
 *
 *     @Composable
 *     override fun render(state: DetailState) { ... }
 * }
 * ```
 */
abstract class AbstractMviActivity<
    I : MviIntent,
    S : MviViewState,
    E : MviSingleEvent,
    VM : MviViewModel<I, S, E>,
    > : ComponentActivity(), MviView<I, S, E> {

    protected abstract val vm: VM

    /**
     * 视图意图通道。
     *
     * 与 `AbstractMviViewModel` 同理：用 Channel 而不是 `MutableSharedFlow`，
     * 否则在 `repeatOnLifecycle(STARTED)` 的收集协程启动前调用 [sendIntent]
     * （例如在 `onCreate` 里）会**静默丢失**意图。
     */
    private val intentChannel = Channel<I>(Channel.UNLIMITED)

    final override fun viewIntents(): Flow<I> = intentChannel.receiveAsFlow()

    /** 在 Compose 中发送意图的入口。 */
    protected fun sendIntent(intent: I) {
        if (intentChannel.trySend(intent).isFailure) {
            AppLogger.w("MviView", "intent dropped, channel closed: $intent")
        }
    }

    /**
     * 标题栏配置，默认取 Activity 在 manifest 中的 label。
     *
     * - 首页 / Tab 页：`TitleBarConfig(title = "...", showBack = false)`
     * - 全屏页（闪屏、播放器）：返回 `null` 即不显示标题栏
     */
    protected open fun titleBarConfig(state: S): TitleBarConfig? =
        TitleBarConfig(title = title?.toString().orEmpty())

    /** 标题栏右侧操作区：用 [TitleBarTextAction] / [TitleBarIconAction] / [TitleBarCustomAction]。 */
    @Composable
    protected open fun TitleBarActions(state: S) = Unit

    /** 点击返回。返回 true 表示已自行处理，否则基类执行 [finish]。 */
    protected open fun onTitleBarBackClick(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bindViewModel()
        setContent {
            val state by vm.viewState.collectAsStateWithLifecycle()
            Column(Modifier.fillMaxSize()) {
                titleBarConfig(state)?.let { config ->
                    AppTitleBar(
                        title = config.title,
                        onBack = if (config.showBack) {
                            { if (!onTitleBarBackClick()) finish() }
                        } else {
                            null
                        },
                        backgroundColor = config.backgroundColor
                            ?: MaterialTheme.colorScheme.surface,
                        contentColor = config.contentColor
                            ?: MaterialTheme.colorScheme.onSurface,
                    ) {
                        TitleBarActions(state)
                    }
                }
                Box(Modifier.weight(1f)) {
                    render(state)
                }
            }
        }
    }

    private fun bindViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.singleEvent.collect { handleSingleEvent(it) } }
                launch { viewIntents().collect { vm.processIntent(it) } }
            }
        }
    }
}
