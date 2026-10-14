package com.example.testdemo.lib.flutter

import com.example.testdemo.lib.log.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * EventChannel 事件流桥梁核心实现。
 *
 * 管理 Native 向 Flutter 推送的实时事件流（如扫码、网络变化、推送等）。
 */
class FlutterEventStreamBridge(
    private val mainScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val streamHandlers = ConcurrentHashMap<String, FlutterEventStreamHandler>()
    private val activeEmitters = ConcurrentHashMap<String, FlutterEventEmitter>()

    /**
     * 注册事件流处理器。
     */
    fun registerStreamHandler(handler: FlutterEventStreamHandler) {
        streamHandlers[handler.streamName] = handler
        AppLogger.d("FlutterBridge", "Registered EventChannel stream: ${handler.streamName}")
    }

    /**
     * 当 Flutter 端开启事件流订阅时调用。
     */
    fun onListen(streamName: String, rawSink: (Any?) -> Unit) {
        val handler = streamHandlers[streamName] ?: run {
            AppLogger.w("FlutterBridge", "No EventChannel handler registered for $streamName")
            return
        }

        val emitter = FlutterEventEmitter { event ->
            mainScope.launch {
                rawSink(event)
            }
        }
        activeEmitters[streamName] = emitter
        handler.onListen(emitter)
    }

    /**
     * 当 Flutter 端取消事件流订阅时调用。
     */
    fun onCancel(streamName: String) {
        activeEmitters.remove(streamName)
        streamHandlers[streamName]?.onCancel()
    }

    /**
     * 主动向指定 EventChannel 推送事件。
     */
    fun dispatchEvent(streamName: String, event: Any?) {
        activeEmitters[streamName]?.sendEvent(event)
    }
}
