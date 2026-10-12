package com.example.testdemo.lib.net

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.Logger as KtorLogger

/** 登录态 token 提供者，返回 null 表示未登录。 */
fun interface TokenProvider {
    fun token(): String?
}

data class NetworkConfig(
    val baseUrl: String,
    val connectTimeoutMillis: Long = 15_000,
    val requestTimeoutMillis: Long = 15_000,
    val socketTimeoutMillis: Long = 15_000,

    /**
     * 是否打印 HTTP 日志。
     *
     * ⚠️ **生产环境必须为 false**。调用方应额外用 `BuildConfig.DEBUG` 兜底，
     * 保证 release 包任何情况下都不输出请求内容。
     */
    val enableHttpLogs: Boolean = false,

    /** 日志详细程度，仅 [enableHttpLogs] 为 true 时生效。 */
    val httpLogLevel: HttpLogLevel = HttpLogLevel.BASIC,

    /** 日志出口。Android 传 `LogcatHttpLogger` 即可在 Logcat 看到。 */
    val httpLogger: HttpLogger = HttpLogger.None,

    val tokenProvider: TokenProvider = TokenProvider { null },
    val headerProvider: HeaderProvider = HeaderProvider { emptyMap() },
)

/**
 * 跨端网络客户端（Ktor）。
 *
 * 平台差异只在 [platformHttpEngine]（Android→OkHttp，iOS→Darwin），
 * 其余代码 Android / iOS 完全共用。
 */
class NetworkClient(val config: NetworkConfig, engine: HttpClientEngine = platformHttpEngine().create()) {

    val http: HttpClient = HttpClient(engine) {
        // 不抛异常，失败统一由 ApiResult 表达
        expectSuccess = false

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    isLenient = true
                    explicitNulls = false
                }
            )
        }

        install(HttpTimeout) {
            connectTimeoutMillis = config.connectTimeoutMillis
            requestTimeoutMillis = config.requestTimeoutMillis
            socketTimeoutMillis = config.socketTimeoutMillis
        }

        if (config.enableHttpLogs && config.httpLogLevel.enabled) {
            install(Logging) {
                // 交给平台日志出口（Android → Logcat），而不是 Ktor 默认的 println
                logger = object : KtorLogger {
                    override fun log(message: String) {
                        config.httpLogger.log(message.redactSensitive())
                    }
                }
                level = config.httpLogLevel.toKtorLevel()
            }
        }

        defaultRequest {
            url(config.baseUrl)
            header(HttpHeaders.Accept, ContentType.Application.Json)
            config.headerProvider.headers().forEach { (key, value) -> header(key, value) }
            config.tokenProvider.token()
                ?.takeIf { it.isNotBlank() }
                ?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

    fun close() = http.close()
}

/** 自定义等级 → Ktor 等级（不把 Ktor 类型暴露成公共 API）。 */
private fun HttpLogLevel.toKtorLevel(): LogLevel = when (this) {
    HttpLogLevel.NONE -> LogLevel.NONE
    HttpLogLevel.BASIC -> LogLevel.INFO
    HttpLogLevel.HEADERS -> LogLevel.HEADERS
    HttpLogLevel.BODY -> LogLevel.BODY
    HttpLogLevel.ALL -> LogLevel.ALL
}

/** 拼接完整地址，不依赖 Ktor 的相对路径解析细节。 */
fun NetworkClient.url(path: String): String =
    config.baseUrl.trimEnd('/') + "/" + path.trimStart('/')

/**
 * 统一 GET：把网络异常与 HTTP 错误码都收敛成 [ApiResult]。
 *
 * ```
 * val result: ApiResult<List<PostDto>> = networkClient.getResult("posts")
 * ```
 */
suspend inline fun <reified T> NetworkClient.getResult(
    path: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
): ApiResult<T> = try {
    val response: HttpResponse = http.get(url(path)) { block() }
    response.toApiResult()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    ApiResult.Failure(ErrorMapper.fromThrowable(e))
}

/** 把 [HttpResponse] 转成 [ApiResult]，非 2xx 映射成结构化错误。 */
suspend inline fun <reified T> HttpResponse.toApiResult(): ApiResult<T> {
    if (!status.isSuccess()) {
        val text = runCatching { bodyAsText() }.getOrNull()
        return ApiResult.Failure(ErrorMapper.fromHttp(status.value, text))
    }
    return ApiResult.Success(body<T>())
}

/** JSON POST，保留协程取消语义。 */
suspend inline fun <reified B : Any, reified T> NetworkClient.postResult(
    path: String, body: B,
): ApiResult<T> = try {
    http.post(url(path)) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }.toApiResult<T>()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    ApiResult.Failure(ErrorMapper.fromThrowable(e))
}

fun <T> ApiResult<ApiEnvelope<T>>.unwrapEnvelope(): ApiResult<T> = when (this) {
    is ApiResult.Success -> when {
        !data.result -> ApiResult.Failure(ErrorMapper.fromEnvelope(data))
        else -> data.data?.let { ApiResult.Success(it) } ?: ApiResult.Failure(ApiError.emptyBody())
    }
    is ApiResult.Failure -> this
    ApiResult.Cancelled -> ApiResult.Cancelled
}
