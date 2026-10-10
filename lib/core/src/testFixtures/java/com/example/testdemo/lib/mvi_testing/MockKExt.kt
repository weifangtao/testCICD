package com.example.testdemo.lib.mvi_testing

import io.mockk.MockKAdditionalAnswerScope
import io.mockk.MockKStubScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * 让 mockk 的挂起函数延迟 1ms 返回。
 *
 * 规避 [kotlinx.coroutines#3120](https://github.com/Kotlin/kotlinx.coroutines/issues/3120)：
 * mock 瞬时返回时，`Loading -> Success` 之间没有挂起点，StateFlow 会把中间态
 * **合流**掉，导致断言观察不到 Loading。
 *
 * 移植自参考架构的 `mvi-testing`。
 *
 * 用法：
 * ```
 * coEvery { repository.load() } returnsWithDelay ApiResult.Success(data)
 * ```
 */
infix fun <T, B> MockKStubScope<T, B>.returnsWithDelay(
    returnValue: T,
): MockKAdditionalAnswerScope<T, B> = coAnswers {
    delay(1)
    returnValue
}

/** Flow 每个值之间插入 1ms 延迟，避免测试中被合流。 */
fun <T> Flow<T>.delayEach(): Flow<T> = onEach { delay(1) }
