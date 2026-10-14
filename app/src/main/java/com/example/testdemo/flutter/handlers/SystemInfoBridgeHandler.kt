package com.example.testdemo.flutter.handlers

import android.os.Build
import com.example.testdemo.BuildConfig
import com.example.testdemo.lib.config.AppEnvironmentConfigHolder
import com.example.testdemo.lib.flutter.FlutterBridgeHandler
import com.example.testdemo.lib.flutter.FlutterBridgeResult

/**
 * 提供给 Flutter 获取系统与应用基础信息的桥梁处理器。
 *
 * MethodName: `getSystemInfo`
 */
class SystemInfoBridgeHandler : FlutterBridgeHandler {

    override val method: String = "getSystemInfo"

    override suspend fun handle(arguments: Any?): FlutterBridgeResult<Any?> {
        val currentEnv = AppEnvironmentConfigHolder.current()
        val infoMap = mapOf(
            "appName" to "testDemo",
            "versionName" to BuildConfig.VERSION_NAME,
            "versionCode" to BuildConfig.VERSION_CODE,
            "environmentKey" to currentEnv.key,
            "baseUrl" to currentEnv.baseUrl,
            "androidSdkVersion" to Build.VERSION.SDK_INT,
            "deviceModel" to Build.MODEL,
        )
        return FlutterBridgeResult.Success(infoMap)
    }
}
