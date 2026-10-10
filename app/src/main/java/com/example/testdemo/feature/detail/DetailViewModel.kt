package com.example.testdemo.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.testdemo.lib.base.mvi.AbstractMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/**
 * 参数从 [SavedStateHandle] 取（Activity 的 Intent extra / 路由参数都会进来），
 * 所以既能被独立壳 Activity 用，也能被壳内 Compose 页面用。
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : AbstractMviViewModel<DetailIntent, DetailState, DetailEvent>() {

    private val id: String = savedStateHandle.get<String>(ARG_DETAIL_ID).orEmpty()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val viewState: StateFlow<DetailState> = intents
        .flatMapLatest { intent ->
            when (intent) {
                DetailIntent.ToggleFavorite -> flowOf(DetailPartialChange.ToggleFavorite)
                DetailIntent.Back -> {
                    sendEvent(DetailEvent.NavigateBack)
                    emptyFlow()
                }
            }
        }
        .scan(DetailState(id)) { state, change -> change.reduce(state) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DetailState(id))
}
