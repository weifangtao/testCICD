package com.example.testdemo.lib.base.mvi

/**
 * 通用页面状态包装，适合「加载 → 成功 / 空 / 失败」的简单页面。
 * 复杂页面请直接实现 [MviViewState]。
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val message: String = "暂无数据") : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}

val UiState<*>.isLoading: Boolean get() = this is UiState.Loading
val UiState<*>.isSuccess: Boolean get() = this is UiState.Success

fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data
fun UiState<*>.errorMessage(): String? = (this as? UiState.Error)?.message
