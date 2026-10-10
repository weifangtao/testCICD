package com.example.testdemo.lib.net

/**
 * 跨端 HTTP 日志出口。
 *
 * 各平台提供实现：
 * - Android → Logcat（见 `lib:core` 的 `LogcatHttpLogger`）
 * - iOS → NSLog / OSLog
 *
 * 用接口而不是直接依赖平台日志，是为了让 [NetworkClient] 留在 `commonMain`。
 */
fun interface HttpLogger {

    fun log(message: String)

    companion object {
        /** 什么也不做（默认值，用于生产或测试）。 */
        val None: HttpLogger = HttpLogger { }
    }
}

/**
 * HTTP 日志详细程度。
 *
 * 自己定义而不直接用 Ktor 的 `LogLevel`，避免把 `ktor-client-logging`
 * 泄漏成 `lib:shared` 的公共 API。
 */
enum class HttpLogLevel(val enabled: Boolean) {
    /** 不打印。 */
    NONE(enabled = false),

    /** 只打印请求行、响应状态和耗时 —— **推荐默认值**。 */
    BASIC(enabled = true),

    /** 打印请求/响应头（敏感头已脱敏）。 */
    HEADERS(enabled = true),

    /** 打印请求/响应体。 */
    BODY(enabled = true),

    /** 头和体都打印 —— 排查序列化问题时用。 */
    ALL(enabled = true),
}
