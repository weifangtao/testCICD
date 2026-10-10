package com.example.testdemo.lib.base.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 回归测试：**在订阅者就绪之前派发的意图不能丢**。
 *
 * ## 曾经的 bug
 *
 * `AbstractMviViewModel` 原来用 `MutableSharedFlow<I>(extraBufferCapacity = 64)`
 * 承载意图。`MutableSharedFlow` 在 **replay = 0 且没有任何订阅者** 时，
 * `tryEmit` 会返回 `true` 但把值**静默丢弃**（缓冲只服务于「已存在但跟不上」的订阅者）。
 *
 * 而 `viewState` 是 `stateIn(viewModelScope, SharingStarted.Eagerly, ...)`，
 * 它的收集协程是否**已经**订阅并不确定。Compose 侧的
 * `LaunchedEffect(Unit) { viewModel.dispatch(Load) }` 完全可能先执行。
 *
 * 实测后果：全新安装（数据库为空）打开首页，`Load` 意图凭空消失，
 * 不发任何请求，首屏永远空白 —— 而且没有任何日志或报错。
 *
 * ## 为什么用 `runTest` 默认调度器
 *
 * `TestCoroutineDispatcherRule` 用的是 `UnconfinedTestDispatcher`，
 * 它会让 `stateIn` 的收集**同步**发生，从而**掩盖**这个竞态。
 * 这里刻意用 `runTest` 默认的 `StandardTestDispatcher`：
 * 派发时确实还没有订阅者，用来验证意图被**缓冲**而不是被丢弃。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntentBufferingTest {

    @Test
    fun `intent dispatched before any collector is buffered, not dropped`() = runTest {
        val vm = TestViewModel()

        // ① 先派发 —— 此刻 intents 还没有任何订阅者（等价于 LaunchedEffect 抢跑）
        vm.dispatch(TestIntent.Load)

        // ② 之后才订阅
        val received = mutableListOf<TestIntent>()
        val job = launch { vm.exposedIntents().take(1).toList(received) }
        advanceUntilIdle()

        assertEquals(
            "订阅前派发的意图必须被缓冲，不能被静默丢弃",
            listOf(TestIntent.Load),
            received,
        )
        job.cancel()
    }

    @Test
    fun `intents dispatched before collection keep their order`() = runTest {
        val vm = TestViewModel()

        vm.dispatch(TestIntent.Load)
        vm.dispatch(TestIntent.Refresh)
        vm.dispatch(TestIntent.Load)

        val received = mutableListOf<TestIntent>()
        val job = launch { vm.exposedIntents().take(3).toList(received) }
        advanceUntilIdle()

        assertEquals(
            listOf(TestIntent.Load, TestIntent.Refresh, TestIntent.Load),
            received,
        )
        job.cancel()
    }

    @Test
    fun `intents dispatched after collection are delivered too`() = runTest {
        val vm = TestViewModel()

        val received = mutableListOf<TestIntent>()
        val job = launch { vm.exposedIntents().take(2).toList(received) }
        advanceUntilIdle()

        vm.dispatch(TestIntent.Load)
        vm.dispatch(TestIntent.Refresh)
        advanceUntilIdle()

        assertEquals(listOf(TestIntent.Load, TestIntent.Refresh), received)
        job.cancel()
    }

    // ── 测试替身 ──

    private sealed interface TestIntent : MviIntent {
        data object Load : TestIntent
        data object Refresh : TestIntent
    }

    private data class TestState(val count: Int = 0) : MviViewState

    private sealed interface TestEvent : MviSingleEvent

    private class TestViewModel : AbstractMviViewModel<TestIntent, TestState, TestEvent>() {

        // 这里不收集 intents —— 正是为了验证「无订阅者时也不丢」
        override val viewState: StateFlow<TestState> = MutableStateFlow(TestState())

        fun exposedIntents(): Flow<TestIntent> = intents
    }
}
