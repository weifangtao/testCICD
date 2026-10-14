package com.example.testdemo.flutter

import android.content.Context
import com.example.testdemo.AppApplication
import com.example.testdemo.flutter.handlers.GetUserSessionBridgeHandler
import com.example.testdemo.flutter.handlers.LogoutBridgeHandler
import com.example.testdemo.flutter.handlers.ScannerEventStreamHandler
import com.example.testdemo.flutter.handlers.SystemInfoBridgeHandler
import com.example.testdemo.lib.flutter.FlutterEngineManager

/**
 * 混合架构初始化入口。
 *
 * 注册所有的 MethodChannel 和 EventChannel 处理器，并可预热 Flutter 引擎。
 */
object AppFlutterBridgeInitializer {

    val scannerStreamHandler = ScannerEventStreamHandler()

    fun init(app: AppApplication) {
        // 1. 预热 Flutter 引擎
        FlutterEngineManager.prewarmEngine(context = app)

        // 2. 注册 MethodChannel 处理器
        FlutterEngineManager.registerMethodHandler(SystemInfoBridgeHandler())
        FlutterEngineManager.registerMethodHandler(GetUserSessionBridgeHandler(app.sessions))
        FlutterEngineManager.registerMethodHandler(LogoutBridgeHandler(app.sessions))

        // 3. 注册 EventChannel 事件流
        FlutterEngineManager.registerEventStreamHandler(scannerStreamHandler)
    }
}
