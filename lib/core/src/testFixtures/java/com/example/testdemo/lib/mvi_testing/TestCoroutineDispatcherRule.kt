package com.example.testdemo.lib.mvi_testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * 用测试调度器替换 `Dispatchers.Main`，让 `viewModelScope` 在单测里同步执行。
 *
 * 移植自参考架构的 `test-utils/TestCoroutineDispatcherRule`。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestCoroutineDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
        super.starting(description)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
        super.finished(description)
    }
}
