package com.example.testdemo.lib.base.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
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
 * MVI Fragment 基类（Compose 版），行为与 [AbstractMviActivity] 一致：
 * 统一标题栏 + 状态/事件/意图自动绑定。
 *
 * 订阅挂在 `viewLifecycleOwner` 上，视图销毁自动解绑。
 */
abstract class AbstractMviFragment<
    I : MviIntent,
    S : MviViewState,
    E : MviSingleEvent,
    VM : MviViewModel<I, S, E>,
    > : Fragment(), MviView<I, S, E> {

    protected abstract val vm: VM

    /** 同 [AbstractMviActivity]：用 Channel 保证意图不丢。 */
    private val intentChannel = Channel<I>(Channel.UNLIMITED)

    final override fun viewIntents(): Flow<I> = intentChannel.receiveAsFlow()

    protected fun sendIntent(intent: I) {
        if (intentChannel.trySend(intent).isFailure) {
            AppLogger.w("MviView", "intent dropped, channel closed: $intent")
        }
    }

    /** 标题栏配置，默认不显示（Fragment 常嵌在已有标题栏的页面里）。 */
    protected open fun titleBarConfig(state: S): TitleBarConfig? = null

    @Composable
    protected open fun TitleBarActions(state: S) = Unit

    /** 点击返回。返回 true 表示已自行处理，否则基类执行 `popBackStack()`。 */
    protected open fun onTitleBarBackClick(): Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            val state by vm.viewState.collectAsStateWithLifecycle()
            Column(Modifier.fillMaxSize()) {
                titleBarConfig(state)?.let { config ->
                    AppTitleBar(
                        title = config.title,
                        onBack = if (config.showBack) {
                            {
                                if (!onTitleBarBackClick()) {
                                    parentFragmentManager.popBackStack()
                                }
                            }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.singleEvent.collect { handleSingleEvent(it) } }
                launch { viewIntents().collect { vm.processIntent(it) } }
            }
        }
    }
}
