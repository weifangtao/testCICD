package com.example.testdemo.feature.home

import com.example.testdemo.data.remote.PostDto
import com.example.testdemo.lib.base.mvi.MviIntent
import com.example.testdemo.lib.base.mvi.MviSingleEvent
import com.example.testdemo.lib.base.mvi.MviViewState

/** 用户意图 */
sealed interface HomeIntent : MviIntent {
    /** 进入页面：先展示本地数据，同时后台刷新。 */
    data object Load : HomeIntent

    /** 下拉 / 点击重试。 */
    data object Refresh : HomeIntent
}

/**
 * 页面状态。
 *
 * 注意 [posts] **只由数据库推送**（单一数据源），
 * [isLoading] / [error] / [loaded] 只由刷新流程维护。
 */
data class HomeState(
    val posts: List<PostDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** 首次刷新是否已结束（用于区分「还没加载」和「确实没数据」）。 */
    val loaded: Boolean = false,
) : MviViewState {
    /** 刷新失败但有本地数据 → 处于离线缓存模式。 */
    val isOfflineWithCache: Boolean get() = error != null && posts.isNotEmpty()
}

/** 一次性事件 */
sealed interface HomeEvent : MviSingleEvent {
    data class ShowMessage(val message: String) : HomeEvent
}

/** 刷新过程的局部状态变更（不含 posts —— 那是数据库的事）。 */
internal sealed interface HomePartialChange {
    data object Refreshing : HomePartialChange
    data object Refreshed : HomePartialChange
    data class Failed(val message: String) : HomePartialChange

    fun reduce(state: HomeState): HomeState = when (this) {
        Refreshing -> state.copy(isLoading = true, error = null)
        Refreshed -> state.copy(isLoading = false, error = null, loaded = true)
        is Failed -> state.copy(isLoading = false, error = message, loaded = true)
    }
}
