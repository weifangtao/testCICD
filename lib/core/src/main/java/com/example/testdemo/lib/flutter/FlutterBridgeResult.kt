package com.example.testdemo.lib.flutter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Native 与 Flutter 通信统一结果协议。
 */
sealed class FlutterBridgeResult<out T> {
    data class Success<out T>(val data: T) : FlutterBridgeResult<T>()
    data class Error(val code: String, val message: String, val details: Any? = null) : FlutterBridgeResult<Nothing>()
}

/**
 * 通用 JSON 序列化报文，供跨进程 / 跨语言转换使用。
 */
@Serializable
data class FlutterBridgePayload(
    val action: String,
    val params: JsonElement? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * 通用响应报文结构。
 */
@Serializable
data class FlutterBridgeResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
)
