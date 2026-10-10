package com.example.testdemo.lib.base.mvi

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/**
 * 视图侧契约：渲染状态、处理一次性事件、向上抛意图。
 *
 * @param I [MviIntent] 顶层类型
 * @param S [MviViewState] 顶层类型
 * @param E [MviSingleEvent] 顶层类型
 */
interface MviView<I : MviIntent, S : MviViewState, E : MviSingleEvent> {

    /** 依据状态渲染 UI。 */
    @Composable
    fun render(state: S)

    /** 处理一次性事件（Snackbar / 导航 / 弹窗）。 */
    fun handleSingleEvent(event: E)

    /** 视图产生的意图流，ViewModel 唯一的事件来源。 */
    fun viewIntents(): Flow<I>
}
