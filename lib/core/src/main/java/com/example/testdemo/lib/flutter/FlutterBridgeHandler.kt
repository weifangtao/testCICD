package com.example.testdemo.lib.flutter

/**
 * Flutter MethodChannel 业务处理器接口。
 *
 * 每一个 Native 暴露给 Flutter 的功能模块均实现此接口。
 */
interface FlutterBridgeHandler {

    /** 处理器所属的业务模块名称 / 方法名。 */
    val method: String

    /**
     * 处理 Flutter 发起的调用。
     *
     * @param arguments Flutter 传入的参数（可为 Map、List、String 或 null）
     * @return 返回 [FlutterBridgeResult] 给 Flutter
     */
    suspend fun handle(arguments: Any?): FlutterBridgeResult<Any?>
}

/**
 * Flutter EventChannel 事件流订阅接口。
 *
 * 用于 Android Native 向 Flutter 持续推送事件（如扫码、网络状态、传感器等）。
 */
interface FlutterEventStreamHandler {

    /** 事件流频道名称，例如 `com.example.testdemo/scanner_events`。 */
    val streamName: String

    /** 监听开始：Flutter 端订阅事件流时触发。 */
    fun onListen(emitter: FlutterEventEmitter)

    /** 监听取消：Flutter 端取消订阅时触发。 */
    fun onCancel()
}

/**
 * 事件发射器抽象。
 */
fun interface FlutterEventEmitter {
    fun sendEvent(event: Any?)
}
