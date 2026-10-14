package com.example.testdemo

import android.app.Application
import com.example.testdemo.auth.*
import com.example.testdemo.data.local.AppDatabase
import com.example.testdemo.lib.config.AppEnvironment
import com.example.testdemo.lib.config.AppEnvironmentConfigHolder
import com.example.testdemo.lib.log.AppLogger
import com.example.testdemo.lib.log.CrashGuard
import com.example.testdemo.lib.net.NetworkClient
import com.example.testdemo.lib.net.NetworkConfig
import com.example.testdemo.lib.storage.AppStorage
import com.example.testdemo.lib.storage.createAppStorage
import com.example.testdemo.flutter.AppFlutterBridgeInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*

@HiltAndroidApp
class AppApplication : Application() {
    lateinit var installationStorage: AppStorage
        private set
    lateinit var sessions: SessionManager
        private set
    lateinit var wms: WmsGateway
        private set
    private val stores = mutableMapOf<String, AppStorage>()
    private val databases = mutableMapOf<String, AppDatabase>()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val scopeKey: String get() = sessions.state.value.session?.scopeKey
        ?: "${AppEnvironmentConfigHolder.current().key}_guest"
    val storage: AppStorage get() = synchronized(stores) {
        stores.getOrPut(scopeKey) { createAppStorage(this, "business_$scopeKey") }
    }
    val database: AppDatabase get() = synchronized(databases) {
        databases.getOrPut(scopeKey) { AppDatabase.create(this, "business_$scopeKey.db") }
    }

    /** 公开能力示例独立客户端，永远不携带 WMS Token。 */
    val network by lazy { NetworkClient(NetworkConfig("https://jsonplaceholder.typicode.com/")) }

    override fun onCreate() {
        super.onCreate()
        AppEnvironmentConfigHolder.initialize(AppEnvironment.fromKey(BuildConfig.APP_ENV))
        AppLogger.enabled = BuildConfig.DEBUG
        CrashGuard.install { AppLogger.e("App", "crash captured", it) }
        installationStorage = createAppStorage(this, "installation")
        val cipher = SessionCipher()
        sessions = SessionManager(installationStorage, cipher::encrypt, cipher::decrypt)
        wms = WmsGateway(sessions)
        AppFlutterBridgeInitializer.init(this)
        applicationScope.launch { sessions.restore() }
    }
}
