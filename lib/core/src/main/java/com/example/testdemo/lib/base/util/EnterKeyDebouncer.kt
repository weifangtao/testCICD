package com.example.testdemo.lib.base.util

import android.os.SystemClock

/**
 * 扫描枪回车防抖。
 *
 * PDA 扫描头常在极短时间内重复触发回车，用它把重复的 key-up 事件挡掉。
 * 时间源可注入，便于 JVM 单元测试。
 *
 * 移植自 k3_wms_android 的 EnterKeyDebouncer。
 */
class EnterKeyDebouncer(
    debounceIntervalMillis: Long,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
) {
    private var debounceInterval = debounceIntervalMillis.coerceAtLeast(0L)
    private var lastAcceptedAt = Long.MIN_VALUE

    fun setDebounceInterval(millis: Long) {
        debounceInterval = millis.coerceAtLeast(0L)
    }

    /** @return true 表示本次事件应被忽略。 */
    fun shouldDebounce(): Boolean {
        if (debounceInterval <= 0L) return false
        val now = clock()
        if (lastAcceptedAt != Long.MIN_VALUE && now - lastAcceptedAt < debounceInterval) {
            return true
        }
        lastAcceptedAt = now
        return false
    }
}
