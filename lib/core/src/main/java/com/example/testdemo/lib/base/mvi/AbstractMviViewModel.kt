package com.example.testdemo.lib.base.mvi

import android.os.Build
import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testdemo.lib.log.AppLogger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn

/**
 * MVI ViewModel 基类（对应参考架构的 `AbstractMviViewModel`）。
 *
 * 单向数据流：
 * ```
 * View --(Intent)--> intents --> 业务流 --> scan(reduce) --> viewState --> View
 *                                 └--> sendEvent --> singleEvent --> View
 * ```
 *
 * - 意图用 [Channel] 汇聚（**不会丢**，原因见 [intentChannel]）
 * - 一次性事件用 [Channel] 投递，保证「有且仅有一次」，不会像状态那样重放
 * - [logTag] 自动取类名并按需截断
 */
abstract class AbstractMviViewModel<I : MviIntent, S : MviViewState, E : MviSingleEvent> :
    ViewModel(), MviViewModel<I, S, E> {

    /** 自定义日志 tag，默认取类名。 */
    protected open val rawLogTag: String? = null

    protected val logTag: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        (rawLogTag ?: this::class.java.simpleName).let { tag ->
            // API 26 之前 Log tag 有 23 字符上限
            if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) tag
            else tag.take(MAX_TAG_LENGTH)
        }
    }

    private val eventChannel = Channel<E>(Channel.UNLIMITED)

    /**
     * 意图通道。
     *
     * ⚠️ **这里必须用 Channel，不能用 `MutableSharedFlow(replay = 0)`。**
     *
     * `MutableSharedFlow` 在**没有订阅者**时 `tryEmit` 会返回 true 但**静默丢弃**值。
     * 而 `viewState` 是 `stateIn(viewModelScope, Eagerly, ...)`，
     * 它的收集协程是**异步启动**的；Compose 侧
     * `LaunchedEffect { viewModel.dispatch(Load) }` 完全可能先跑完 ——
     * 结果是首次加载意图凭空消失，首屏永远停在空状态。
     *
     * Channel 会把意图缓冲到订阅者到来为止，不存在这个竞态。
     */
    private val intentChannel = Channel<I>(Channel.UNLIMITED)

    final override val singleEvent: Flow<E> = eventChannel.receiveAsFlow()

    /**
     * 子类在 `viewState` 里订阅它来消费意图。
     *
     * 注意这是**单消费者**的 Flow：只应由 `viewState` 收集一次。
     */
    protected val intents: Flow<I> = intentChannel.receiveAsFlow()

    final override suspend fun processIntent(intent: I) {
        intentChannel.send(intent)
    }

    /**
     * 非挂起的意图入口，便于 Compose 的 `onClick` 等非协程场景直接调用。
     *
     * 意图会被缓冲，即使此时还没有订阅者也不会丢。
     */
    fun dispatch(intent: I) {
        val result = intentChannel.trySend(intent)
        if (result.isFailure) {
            // 只有 ViewModel 已销毁（通道关闭）才会走到这里
            AppLogger.w(logTag, "intent dropped, channel closed: $intent")
        }
    }

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        eventChannel.close()
        intentChannel.close()
        AppLogger.d(logTag, "onCleared")
    }

    /**
     * 发送一次性事件。
     *
     * 通道容量无限，仅当 ViewModel 已销毁（通道关闭）时才会失败，此时页面已不存在，忽略即可。
     */
    protected fun sendEvent(event: E) {
        eventChannel.trySend(event)
    }

    /** 调试日志：打印流经的每个值。 */
    protected fun <T> Flow<T>.debugLog(subject: String): Flow<T> =
        onEach { AppLogger.d(logTag, ">>> $subject: $it") }

    /** 在 viewModelScope 中共享：首个订阅者到来时启动，最后一个离开时停止。 */
    protected fun <T> Flow<T>.shareWhileSubscribed(): SharedFlow<T> =
        shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    private companion object {
        const val MAX_TAG_LENGTH = 23
    }
}
