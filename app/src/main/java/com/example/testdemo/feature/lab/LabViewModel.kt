package com.example.testdemo.feature.lab

import androidx.lifecycle.viewModelScope
import com.example.testdemo.lib.base.mvi.AbstractMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.testdemo.lib.base.mvi.UiState
import com.example.testdemo.lib.base.util.EnterKeyDebouncer
import com.example.testdemo.lib.log.AppLogger
import com.example.testdemo.lib.net.ApiEnvelope
import com.example.testdemo.lib.net.ErrorMapper
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/**
 * 「能力演示」页 ViewModel：覆盖 lib:base（UiState 四态 / EnterKeyDebouncer）、
 * lib:log（日志 / 崩溃）、lib:net（错误映射）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LabViewModel @Inject constructor() : AbstractMviViewModel<LabIntent, LabState, LabEvent>() {

    private val debouncer = EnterKeyDebouncer(SCAN_DEBOUNCE_MILLIS)

    override val viewState: StateFlow<LabState> = intents
        .flatMapLatest(::handle)
        .scan(LabState()) { state, change -> change.reduce(state) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LabState())

    private fun handle(intent: LabIntent): Flow<LabPartialChange> = when (intent) {
        LabIntent.ShowIdle -> flowOf(LabPartialChange.UiStateChanged(UiState.Idle))
        LabIntent.ShowLoading -> flowOf(LabPartialChange.UiStateChanged(UiState.Loading))
        LabIntent.ShowSuccess ->
            flowOf(LabPartialChange.UiStateChanged(UiState.Success("这里模拟接口返回的内容")))
        LabIntent.ShowEmpty ->
            flowOf(LabPartialChange.UiStateChanged(UiState.Empty("没有查询到符合条件的数据")))
        LabIntent.ShowError ->
            flowOf(LabPartialChange.UiStateChanged(UiState.Error("请求失败，点击重试")))

        is LabIntent.WriteLog -> flow {
            val message = "演示日志 level=${intent.level}"
            when (intent.level) {
                "d" -> AppLogger.d(logTag, message)
                "i" -> AppLogger.i(logTag, message)
                "w" -> AppLogger.w(logTag, message)
                else -> AppLogger.e(logTag, message)
            }
            emit(LabPartialChange.LogWritten(message))
        }

        LabIntent.SimulateScanBurst -> flow {
            // 连发 5 次、间隔小于防抖窗口 → 只有第 1 次被接受
            var accepted = 0
            var blocked = 0
            repeat(5) { if (debouncer.shouldDebounce()) blocked++ else accepted++ }
            emit(LabPartialChange.ScanResult(accepted, blocked))
        }

        is LabIntent.MapError -> flowOf(LabPartialChange.ErrorMapped(mapError(intent.kind)))

        LabIntent.Crash -> flow {
            AppLogger.w(logTag, "即将触发测试崩溃")
            throw IllegalStateException("演示用测试崩溃：验证 CrashGuard 与上报链路")
        }
    }

    private fun mapError(kind: String) = when (kind) {
        "timeout" -> ErrorMapper.fromThrowable(SocketTimeoutException("timeout"))
        "parse" -> ErrorMapper.fromThrowable(SerializationException("bad json"))
        "connect" -> ErrorMapper.fromThrowable(IOException("connect failed"))
        "http" -> ErrorMapper.fromHttp(500, null)
        else -> ErrorMapper.fromEnvelope(
            ApiEnvelope<Any>(result = false, message = "库存不足", code = "1001")
        )
    }.let { error -> "${error.type} / code=${error.code ?: "-"} / ${error.message}" }

    companion object {
        private const val SCAN_DEBOUNCE_MILLIS = 80L

    }
}
