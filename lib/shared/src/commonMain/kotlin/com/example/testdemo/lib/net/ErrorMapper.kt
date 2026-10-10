package com.example.testdemo.lib.net

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.serialization.JsonConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * 把异常 / HTTP 状态码 / 业务响应体统一映射成 [ApiError]。
 *
 * 用的是 Ktor 与 kotlinx.serialization 的异常类型，所以 **Android / iOS 共用**。
 * （原先基于 `java.net.*` 的版本只能在 JVM 上用。）
 */
object ErrorMapper {

    fun fromThrowable(throwable: Throwable): ApiError = when (throwable) {
        // 注意：SocketTimeoutException 继承 IOException，必须排在前面
        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException,
        -> ApiError.timeout()

        is JsonConvertException,
        is SerializationException,
        -> ApiError.parse()

        is IOException -> ApiError.connect()

        else -> ApiError.unknown(throwable.message)
    }

    fun fromHttp(code: Int, body: String?): ApiError = ApiError(
        type = ApiErrorType.HTTP,
        message = body?.takeIf { it.isNotBlank() } ?: "服务器不稳定，请稍后重试（$code）",
        code = code.toString(),
    )

    fun fromEnvelope(envelope: ApiEnvelope<*>?): ApiError =
        if (envelope == null) ApiError.emptyBody()
        else ApiError.business(envelope.message, envelope.code)
}
