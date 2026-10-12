package com.example.testdemo.lib.domain

import kotlinx.coroutines.flow.Flow

/**
 * 单次挂起执行的抽象用例（Domain UseCase）。
 *
 * 业务逻辑收敛于 UseCase，ViewModel 仅负责 UI 状态维护与 Intent 响应。
 */
interface SuspendUseCase<in Params, out Result> {
    suspend operator fun invoke(params: Params): Result
}

/**
 * 无参数的单次挂起用例。
 */
interface NoParamsSuspendUseCase<out Result> {
    suspend operator fun invoke(): Result
}

/**
 * 响应式数据流用例（Flow Domain UseCase）。
 */
interface FlowUseCase<in Params, out Result> {
    operator fun invoke(params: Params): Flow<Result>
}

/**
 * 无参数的响应式数据流用例。
 */
interface NoParamsFlowUseCase<out Result> {
    operator fun invoke(): Flow<Result>
}
