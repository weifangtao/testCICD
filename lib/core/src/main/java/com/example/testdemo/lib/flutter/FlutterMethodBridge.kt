package com.example.testdemo.lib.flutter

import com.example.testdemo.lib.log.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * MethodChannel 通信桥梁核心实现。
 *
 * 1. 注册 / 调度处理 Flutter 调 Native 的请求（自动切协程，防止阻塞 UI）。
 * 2. 提供 Native 主动调用 Flutter 方法的能力。
 */
class FlutterMethodBridge(
    val channelName: String = "com.example.testdemo/bridge",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val handlers = ConcurrentHashMap<String, FlutterBridgeHandler>()

    /**
     * 注册方法处理器。
     */
    fun registerHandler(handler: FlutterBridgeHandler) {
        handlers[handler.method] = handler
        AppLogger.d("FlutterBridge", "Registered MethodChannel handler: ${handler.method}")
    }

    /**
     * 注销方法处理器。
     */
    fun unregisterHandler(method: String) {
        handlers.remove(method)
    }

    /**
     * 接收并分发来自 Flutter 的方法调用。
     *
     * @param method 方法名
     * @param arguments 参数
     * @param callback 结果回调
     */
    fun onMethodCall(method: String, arguments: Any?, callback: (FlutterBridgeResult<Any?>) -> Unit) {
        val handler = handlers[method]
        if (handler == null) {
            callback(
                FlutterBridgeResult.Error(
                    code = "METHOD_NOT_FOUND",
                    message = "Native handler for method '$method' is not registered.",
                )
            )
            return
        }

        coroutineScope.launch {
            try {
                val result = withContext(ioDispatcher) {
                    handler.handle(arguments)
                }
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            } catch (e: Exception) {
                AppLogger.e("FlutterBridge", "Error executing handler for $method", e)
                withContext(Dispatchers.Main) {
                    callback(
                        FlutterBridgeResult.Error(
                            code = "EXECUTION_ERROR",
                            message = e.localizedMessage ?: "Unknown error occurred",
                        )
                    )
                }
            }
        }
    }
}
