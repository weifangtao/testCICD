package com.example.testdemo.feature.detail

import com.example.testdemo.lib.base.mvi.MviIntent
import com.example.testdemo.lib.base.mvi.MviSingleEvent
import com.example.testdemo.lib.base.mvi.MviViewState

/** 路由参数 key：既用于 Intent extra，也用于 SavedStateHandle。 */
const val ARG_DETAIL_ID = "id"

sealed interface DetailIntent : MviIntent {
    data object ToggleFavorite : DetailIntent
    data object Back : DetailIntent
}

data class DetailState(
    val id: String,
    val favorite: Boolean = false,
) : MviViewState

sealed interface DetailEvent : MviSingleEvent {
    data object NavigateBack : DetailEvent
}

internal sealed interface DetailPartialChange {
    data object ToggleFavorite : DetailPartialChange

    fun reduce(state: DetailState): DetailState = when (this) {
        ToggleFavorite -> state.copy(favorite = !state.favorite)
    }
}
