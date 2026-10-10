package com.example.testdemo.lib.mvi_testing

import org.junit.Assert.assertEquals

/**
 * 期望值：既可直接给确定值，也可给断言 lambda。
 *
 * 作用等价于参考架构里 Arrow 的 `Either<(T) -> Unit, T>`，
 * 但用 Kotlin 自带的 sealed interface 实现，少一个依赖。
 */
sealed interface Expected<T> {

    fun assertValue(actual: T, message: String)

    /** 精确相等。 */
    class Exact<T>(private val expected: T) : Expected<T> {
        override fun assertValue(actual: T, message: String) {
            assertEquals(message, expected, actual)
        }
    }

    /** 自定义断言：列表顺序无关、时间戳、浮点误差等场景。 */
    class Assert<T>(private val check: (T) -> Unit) : Expected<T> {
        override fun assertValue(actual: T, message: String) {
            try {
                check(actual)
            } catch (e: AssertionError) {
                throw AssertionError("$message -> ${e.message}", e)
            }
        }
    }
}

/** 精确匹配。 */
fun <T> expectExact(value: T): Expected<T> = Expected.Exact(value)

/** 自定义断言。 */
fun <T> expectThat(check: (T) -> Unit): Expected<T> = Expected.Assert(check)
