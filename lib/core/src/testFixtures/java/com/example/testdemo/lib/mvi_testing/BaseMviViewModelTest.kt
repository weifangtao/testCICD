package com.example.testdemo.lib.mvi_testing

import com.example.testdemo.lib.base.mvi.MviIntent
import com.example.testdemo.lib.base.mvi.MviSingleEvent
import com.example.testdemo.lib.base.mvi.MviViewModel
import com.example.testdemo.lib.base.mvi.MviViewState
import io.mockk.clearAllMocks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import java.util.LinkedList

/**
 * ViewModel 单测基类（移植自参考架构的 `mvi-testing` 模块）。
 *
 * 用法：
 * ```
 * class DetailViewModelTest :
 *     BaseMviViewModelTest<DetailIntent, DetailState, DetailEvent, DetailViewModel>() {
 *
 *     @Test
 *     fun `toggle favorite`() = runVMTest(
 *         vmProducer = { DetailViewModel("42") },
 *         intents = flowOf(DetailIntent.ToggleFavorite),
 *         expectedStates = listOf(
 *             expectExact(DetailState("42")),
 *             expectExact(DetailState("42", favorite = true)),
 *         ),
 *     )
 * }
 * ```
 *
 * [runVMTest] 会依次：订阅 `viewState` / `singleEvent` → 逐个派发 `intents`
 * → 断言状态与事件序列与期望**数量一致、顺序一致、内容匹配**。
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseMviViewModelTest<
    I : MviIntent,
    S : MviViewState,
    E : MviSingleEvent,
    VM : MviViewModel<I, S, E>,
    > {

    @get:Rule
    val coroutineRule = TestCoroutineDispatcherRule()

    /** 打开后打印状态/事件流转过程，便于定位断言失败。 */
    protected open val loggingEnabled: Boolean = false

    @Before
    open fun setup() = Unit

    @After
    open fun tearDown() {
        clearAllMocks()
    }

    protected fun runVMTest(
        vmProducer: () -> VM,
        intents: Flow<I>,
        expectedStates: List<Expected<S>>,
        expectedEvents: List<Expected<E>> = emptyList(),
        preProcessingIntents: Flow<I>? = null,
        otherAssertions: (suspend () -> Unit)? = null,
    ) = runTest(coroutineRule.testDispatcher) {
        val vm = vmProducer()
        preProcessingIntents?.let { prepare(vm, it) }

        val states = LinkedList<S>()
        val events = LinkedList<E>()

        val stateJob = vm.viewState
            .onEach { state ->
                log { "[STATE] <- $state" }
                states += state
            }
            .launchIn(this)

        val eventJob = vm.singleEvent
            .onEach { event ->
                log { "[EVENT] <- $event" }
                events += event
            }
            .launchIn(this)

        intents.collect {
            log { "[DISPATCH] $it" }
            vm.processIntent(it)
        }

        advanceUntilIdle()
        runCurrent()

        stateJob.cancelAndJoin()
        eventJob.cancelAndJoin()

        log { "[DONE] states=$states" }
        log { "[DONE] events=$events" }

        assertEquals("States size", expectedStates.size, states.size)
        assertEquals("Events size", expectedEvents.size, events.size)

        expectedStates.withIndex().zip(states).forEach { (indexed, actual) ->
            val (index, expected) = indexed
            expected.assertValue(actual, "[State index=$index]")
        }
        expectedEvents.withIndex().zip(events).forEach { (indexed, actual) ->
            val (index, expected) = indexed
            expected.assertValue(actual, "[Event index=$index]")
        }

        otherAssertions?.invoke()
    }

    /** 先派发一批「预热」意图并等其完成，再进入正式断言，用于覆盖初始化分支。 */
    private suspend fun TestScope.prepare(vm: VM, intents: Flow<I>) {
        val stateJob = vm.viewState.launchIn(this)
        val eventJob = vm.singleEvent.launchIn(this)
        intents
            .onCompletion {
                advanceUntilIdle()
                runCurrent()
                stateJob.cancelAndJoin()
                eventJob.cancelAndJoin()
            }
            .collect {
                log { "[BEFORE] $it" }
                vm.processIntent(it)
            }
    }

    private fun log(message: () -> String) {
        if (loggingEnabled) println(message())
    }
}
