package com.example.testdemo.feature.lab

import com.example.testdemo.lib.base.mvi.MviIntent
import com.example.testdemo.lib.base.mvi.MviSingleEvent
import com.example.testdemo.lib.base.mvi.MviViewState
import com.example.testdemo.lib.base.mvi.UiState

sealed interface LabIntent : MviIntent {
    data object ShowIdle : LabIntent
    data object ShowLoading : LabIntent
    data object ShowSuccess : LabIntent
    data object ShowEmpty : LabIntent
    data object ShowError : LabIntent
    data class WriteLog(val level: String) : LabIntent
    data object SimulateScanBurst : LabIntent
    data class MapError(val kind: String) : LabIntent
    data object Crash : LabIntent
}

data class LabState(
    val uiState: UiState<String> = UiState.Idle,
    val lastLog: String = "（还没有写过日志）",
    val scanAccepted: Int = 0,
    val scanBlocked: Int = 0,
    val errorMapping: String = "（点上面的按钮看映射结果）",
) : MviViewState

sealed interface LabEvent : MviSingleEvent {
    data class ShowMessage(val message: String) : LabEvent
}

internal sealed interface LabPartialChange {
    data class UiStateChanged(val uiState: UiState<String>) : LabPartialChange
    data class LogWritten(val message: String) : LabPartialChange
    data class ScanResult(val accepted: Int, val blocked: Int) : LabPartialChange
    data class ErrorMapped(val text: String) : LabPartialChange

    fun reduce(state: LabState): LabState = when (this) {
        is UiStateChanged -> state.copy(uiState = uiState)
        is LogWritten -> state.copy(lastLog = message)
        is ScanResult -> state.copy(
            scanAccepted = state.scanAccepted + accepted,
            scanBlocked = state.scanBlocked + blocked,
        )
        is ErrorMapped -> state.copy(errorMapping = text)
    }
}
