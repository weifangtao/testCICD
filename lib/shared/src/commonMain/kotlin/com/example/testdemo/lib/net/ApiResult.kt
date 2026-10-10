package com.example.testdemo.lib.net

/** 统一的请求结果：成功 / 失败（带结构化错误）/ 已取消。 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
    data object Cancelled : ApiResult<Nothing>
}

val ApiResult<*>.isSuccess: Boolean get() = this is ApiResult.Success

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data

fun ApiResult<*>.errorMessage(): String = (this as? ApiResult.Failure)?.error?.message.orEmpty()

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
    ApiResult.Cancelled -> ApiResult.Cancelled
}

inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

inline fun <T> ApiResult<T>.onFailure(action: (ApiError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) action(error)
    return this
}
