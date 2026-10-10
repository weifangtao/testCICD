package com.example.testdemo.lib.base.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel 侧契约：消费意图、产出状态与一次性事件。
 */
interface MviViewModel<I : MviIntent, S : MviViewState, E : MviSingleEvent> {

    val viewState: StateFlow<S>

    val singleEvent: Flow<E>

    suspend fun processIntent(intent: I)
}
