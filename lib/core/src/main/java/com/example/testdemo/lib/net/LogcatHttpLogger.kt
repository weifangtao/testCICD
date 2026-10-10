package com.example.testdemo.lib.net

import com.example.testdemo.lib.log.AppLogger

/**
 * 把 Ktor 的 HTTP 日志写进 **Logcat**。
 *
 * 单独过滤：
 * ```
 * adb logcat -s Http
 * ```
 *
 * 说明：
 * - 走 [AppLogger]，因此额外受 `AppLogger.enabled` 控制（release 包为 false）
 * - 逐行输出，避免 Logcat 把多行消息折叠
 * - 超长行（响应体）按 [MAX_LOG_CHUNK] 分块，避免被 Logcat 截断
 */
object LogcatHttpLogger : HttpLogger {

    /** Logcat tag，固定值方便过滤。 */
    const val TAG = "Http"

    private const val MAX_LOG_CHUNK = 3_000

    override fun log(message: String) {
        message.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach
            if (line.length <= MAX_LOG_CHUNK) {
                AppLogger.d(TAG, line)
            } else {
                line.chunked(MAX_LOG_CHUNK).forEach { chunk -> AppLogger.d(TAG, chunk) }
            }
        }
    }
}
