package com.example.testdemo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.example.testdemo.data.local.AppDatabase
import com.example.testdemo.lib.config.AppEnvironment
import com.example.testdemo.lib.config.AppEnvironmentConfigHolder
import com.example.testdemo.lib.log.AppLogger
import com.example.testdemo.lib.log.CrashGuard
import com.example.testdemo.lib.net.HttpLogLevel
import com.example.testdemo.lib.net.LogcatHttpLogger
import com.example.testdemo.lib.net.NetworkClient
import com.example.testdemo.lib.net.NetworkConfig
import com.example.testdemo.lib.storage.AppStorage
import com.example.testdemo.lib.storage.createAppStorage

@HiltAndroidApp
class AppApplication : Application() {

    lateinit var storage: AppStorage
        private set

    /** Room 数据库（结构化数据的本地存储）。 */
    lateinit var database: AppDatabase
        private set

    private var activeNetworkKey: String? = null
    private var activeNetworkClient: NetworkClient? = null

    /**
     * 按当前环境提供唯一客户端。切换环境时关闭旧客户端，避免请求、日志配置和 Token 串用。
     *
     * HTTP 日志的双重开关：
     * 1. `BuildConfig.DEBUG` —— release 包**永远**不打印，这是硬兜底
     * 2. `environment.enableHttpLogs` —— 环境级开关（dev/test 开，prod 关）
     *
     * 日志内容已由 `redactSensitive()` 脱敏，token 不会进 Logcat。
     */
    val network: NetworkClient
        get() = synchronized(this) {
            val environment = AppEnvironmentConfigHolder.current()
            // release 包无论环境如何都不打印请求内容
            val logsEnabled = BuildConfig.DEBUG && environment.enableHttpLogs
            val key = "${environment.key}|${environment.baseUrl}|$logsEnabled"
            if (key != activeNetworkKey) {
                activeNetworkClient?.close()
                activeNetworkClient = NetworkClient(
                    NetworkConfig(
                        baseUrl = environment.baseUrl,
                        enableHttpLogs = logsEnabled,
                        httpLogLevel = HttpLogLevel.ALL,
                        // 写进 Logcat：adb logcat -s Http
                        httpLogger = LogcatHttpLogger,
                        headerProvider = { mapOf("X-App-Version" to BuildConfig.VERSION_NAME) },
                    )
                )
                activeNetworkKey = key
            }
            return@synchronized checkNotNull(activeNetworkClient)
        }

    override fun onCreate() {
        super.onCreate()

        // 1. 环境：Gradle 只提供 APP_ENV 标识，地址与日志开关都在 AppEnvironment 里
        val environment = AppEnvironment.fromKey(BuildConfig.APP_ENV)
        AppEnvironmentConfigHolder.initialize(environment)

        // 2. 日志：debug 包全开，release 包一律关闭（避免业务数据进 Logcat）
        AppLogger.enabled = BuildConfig.DEBUG

        // 3. 崩溃兜底：记日志后仍交给系统默认处理
        CrashGuard.install { throwable ->
            // TODO 接 Bugly：CrashReport.postCatchedException(throwable)
            AppLogger.e("App", "crash captured", throwable)
        }

        // 4. 存储
        storage = createAppStorage(this)

        // 5. 数据库
        database = AppDatabase.create(this)
    }
}
