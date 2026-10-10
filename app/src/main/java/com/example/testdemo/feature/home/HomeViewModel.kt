package com.example.testdemo.feature.home

import androidx.lifecycle.viewModelScope
import com.example.testdemo.data.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.testdemo.lib.base.mvi.AbstractMviViewModel
import com.example.testdemo.lib.net.ApiResult
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/**
 * 单一数据源（SSOT）写法：
 *
 * ```
 * Room Flow ────────────────────┐
 *                               ├─ combine → viewState → UI
 * Intent → 刷新 → 写入 Room ────┘
 * ```
 *
 * 数据库一变，UI 自动更新；刷新只负责把新数据写进库。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PostRepository,
) : AbstractMviViewModel<HomeIntent, HomeState, HomeEvent>() {

    override val viewState: StateFlow<HomeState> =
        combine(
            // ① 刷新流程：只维护 isLoading / error / loaded
            intents
                .debugLog("HomeIntent")
                .flatMapLatest { refreshFlow() }
                .onEach { change ->
                    if (change is HomePartialChange.Failed) {
                        sendEvent(HomeEvent.ShowMessage(change.message))
                    }
                }
                .scan(HomeState()) { state, change -> change.reduce(state) },

            // ② 数据来源：数据库（唯一）
            repository.observePosts(),
        ) { state, posts ->
            state.copy(posts = posts)
        }
            .debugLog("HomeState")
            .stateIn(viewModelScope, SharingStarted.Eagerly, HomeState())

    private fun refreshFlow(): Flow<HomePartialChange> = flow {
        emit(HomePartialChange.Refreshing)
        when (val result = repository.refresh()) {
            is ApiResult.Success -> emit(HomePartialChange.Refreshed)
            is ApiResult.Failure -> emit(HomePartialChange.Failed(result.error.message))
            ApiResult.Cancelled -> Unit
        }
    }

}
