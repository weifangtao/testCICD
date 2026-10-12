package com.example.testdemo.lib.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 环境定义。
 *
 * **接口地址直接写在这里** —— 改地址不用动 Gradle 脚本，
 * 也不用为「同一个环境换地址」重新打包（测试包还能热切换）。
 *
 * @param key 与 `BuildConfig.APP_ENV` 对应的标识
 * @param baseUrl 接口根地址，必须以 `/` 结尾
 * @param enableHttpLogs 是否打印 HTTP 日志。
 *
 * 这是**环境侧**的开关，最终是否输出还取决于调用方：
 * Android 侧会再用 `BuildConfig.DEBUG` 兜底，保证 release 包永远不打印。
 * 日志内容会先经过 [com.example.testdemo.lib.net.redactSensitive] 脱敏，
 * token / Cookie / 密码不会进 Logcat。
 */
enum class AppEnvironment(
    val key: String,
    val baseUrl: String,
    val enableHttpLogs: Boolean,
) {
    DEV(
        key = "dev",
        baseUrl = "https://wms-test.haoqianyi.com/k3_wms/",
        enableHttpLogs = true,
    ),
    TEST(
        key = "test",
        baseUrl = "https://wms-test.haoqianyi.com/k3_wms/",
        enableHttpLogs = true,
    ),
    PROD(
        key = "prod",
        baseUrl = "https://wms.haoqianyi.com/k3_wms/",
        enableHttpLogs = false,
    ),
    ;

    companion object {
        fun fromKey(key: String): AppEnvironment =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: TEST
    }
}

/**
 * 当前环境持有者。
 *
 * - 启动时用 [initialize] 注入 `BuildConfig.APP_ENV` 对应的环境
 * - 测试包可在设置页调用 [switchTo] 热切换，无需重新打包
 */
object AppEnvironmentConfigHolder {

    private val _environment = MutableStateFlow(AppEnvironment.TEST)

    /** 当前环境（可订阅，切换后自动通知）。 */
    val environment: StateFlow<AppEnvironment> = _environment.asStateFlow()

    fun initialize(environment: AppEnvironment) {
        _environment.value = environment
    }

    fun switchTo(environment: AppEnvironment) {
        _environment.value = environment
    }

    fun current(): AppEnvironment = _environment.value
}
