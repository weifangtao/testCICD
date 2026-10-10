package com.example.testdemo.lib.base.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 生命周期感知收集：仅在 [owner] 处于 [minActiveState] 及以上时收集，
 * 退到后台自动停止，避免无谓的订阅与内存泄漏。
 *
 * 移植自参考架构的 `core-ui/CollectIn.kt`。
 */
inline fun <T> Flow<T>.collectIn(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (T) -> Unit,
): Job = owner.lifecycleScope.launch {
    owner.repeatOnLifecycle(minActiveState) {
        collect { action(it) }
    }
}
