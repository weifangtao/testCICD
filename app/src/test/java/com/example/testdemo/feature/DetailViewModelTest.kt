package com.example.testdemo.feature

import androidx.lifecycle.SavedStateHandle
import com.example.testdemo.feature.detail.ARG_DETAIL_ID
import com.example.testdemo.feature.detail.DetailEvent
import com.example.testdemo.feature.detail.DetailIntent
import com.example.testdemo.feature.detail.DetailState
import com.example.testdemo.feature.detail.DetailViewModel
import com.example.testdemo.lib.mvi_testing.BaseMviViewModelTest
import com.example.testdemo.lib.mvi_testing.expectExact
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class DetailViewModelTest :
    BaseMviViewModelTest<DetailIntent, DetailState, DetailEvent, DetailViewModel>() {

    /** 参数现在来自 SavedStateHandle（Activity Intent extra / 路由参数都会进这里）。 */
    private fun detailViewModel(id: String) =
        DetailViewModel(SavedStateHandle(mapOf(ARG_DETAIL_ID to id)))

    @Test
    fun `initial state carries the route id`() = runVMTest(
        vmProducer = { detailViewModel("42") },
        intents = flowOf(),
        expectedStates = listOf(expectExact(DetailState("42"))),
    )

    @Test
    fun `toggle favorite flips the flag twice`() = runVMTest(
        vmProducer = { detailViewModel("42") },
        intents = flowOf(DetailIntent.ToggleFavorite, DetailIntent.ToggleFavorite),
        expectedStates = listOf(
            expectExact(DetailState("42")),
            expectExact(DetailState("42", favorite = true)),
            expectExact(DetailState("42", favorite = false)),
        ),
    )

    @Test
    fun `back emits navigate back event and keeps state`() = runVMTest(
        vmProducer = { detailViewModel("42") },
        intents = flowOf(DetailIntent.Back),
        expectedStates = listOf(expectExact(DetailState("42"))),
        expectedEvents = listOf(expectExact(DetailEvent.NavigateBack)),
    )
}
