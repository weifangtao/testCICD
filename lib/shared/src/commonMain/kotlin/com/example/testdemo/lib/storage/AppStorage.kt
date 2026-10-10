package com.example.testdemo.lib.storage

import kotlinx.coroutines.flow.Flow

/**
 * Key-Value 存储契约（跨端）。
 *
 * Android 侧由 `DataStoreStorage` 实现；iOS 侧可用 NSUserDefaults 实现。
 * 业务只依赖本接口，所以两端共用。
 */
interface AppStorage {
    suspend fun getString(key: String, default: String? = null): String?
    suspend fun putString(key: String, value: String?)

    suspend fun getBoolean(key: String, default: Boolean = false): Boolean
    suspend fun putBoolean(key: String, value: Boolean)

    suspend fun getInt(key: String, default: Int = 0): Int
    suspend fun putInt(key: String, value: Int)

    suspend fun getLong(key: String, default: Long = 0L): Long
    suspend fun putLong(key: String, value: Long)

    suspend fun remove(key: String)
    suspend fun clear()

    /** 观察某个 key 的变化；订阅时先收到当前值。 */
    fun observeString(key: String): Flow<String?>
}
