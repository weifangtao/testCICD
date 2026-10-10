package com.example.testdemo.lib.base.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.testdemo.lib.base.mvi.MviIntent
import com.example.testdemo.lib.base.mvi.MviSingleEvent
import com.example.testdemo.lib.base.mvi.MviViewModel
import com.example.testdemo.lib.base.mvi.MviViewState

/**
 * 在「没有 Activity/Fragment 基类」的场景（例如单 Activity + 路由）
 * 把 [MviViewModel] 接到 Compose 上。
 *
 * - 状态：生命周期感知收集，退到后台自动停止
 * - 事件：仅在 STARTED 及以上状态收集，避免后台消费丢失
 */
@Composable
fun <I : MviIntent, S : MviViewState, E : MviSingleEvent> MviScreen(
    viewModel: MviViewModel<I, S, E>,
    onEvent: (E) -> Unit,
    content: @Composable (S) -> Unit,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val currentOnEvent by rememberUpdatedState(onEvent)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.singleEvent.collect { currentOnEvent(it) }
        }
    }

    content(state)
}
