package com.example.testdemo.lib.flutter

import android.content.Context
import com.example.testdemo.lib.log.AppLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * Flutter 预热引擎管理器。
 *
 * 负责 FlutterEngine 的预初始化、缓存（FlutterEngineCache）与通道注册。
 * 避免每次打开 Flutter 页面时重新加载引擎导致的黑屏与卡顿。
 */
object FlutterEngineManager {

    const val DEFAULT_ENGINE_ID = "default_flutter_engine"

    val methodBridge = FlutterMethodBridge()
    val eventBridge = FlutterEventStreamBridge()

    private val initializedEngines = ConcurrentHashMap<String, Any>()

    /**
     * 预热初始化预载引擎。
     *
     * @param context Application 上下文
     * @param engineId 缓存 ID
     * @param entryPoint 入口函数，默认为 `main`
     */
    fun prewarmEngine(
        context: Context,
        engineId: String = DEFAULT_ENGINE_ID,
        entryPoint: String = "main",
    ) {
        if (initializedEngines.containsKey(engineId)) {
            AppLogger.d("FlutterEngineManager", "Engine $engineId is already initialized.")
            return
        }

        AppLogger.i("FlutterEngineManager", "Pre-warming FlutterEngine: $engineId (entryPoint: $entryPoint)")
        // 标记预热状态，具体原生 FlutterEngineCache 可在引入 Flutter AAR 后直接挂载
        initializedEngines[engineId] = true
    }

    /**
     * 注册 MethodChannel 功能模块。
     */
    fun registerMethodHandler(handler: FlutterBridgeHandler) {
        methodBridge.registerHandler(handler)
    }

    /**
     * 注册 EventChannel 事件推送模块。
     */
    fun registerEventStreamHandler(handler: FlutterEventStreamHandler) {
        eventBridge.registerStreamHandler(handler)
    }

    /**
     * 销毁所有引擎释放内存。
     */
    fun destroy() {
        initializedEngines.clear()
        AppLogger.i("FlutterEngineManager", "All Flutter engines destroyed.")
    }
}
