package com.example.testdemo.lib.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Android 侧实现：基于 **Jetpack DataStore**。
 *
 * 相比 SharedPreferences：全 suspend / Flow，不阻塞主线程；
 * 变更用 Flow 天然表达；有事务语义。
 */
class DataStoreStorage(
    private val dataStore: DataStore<Preferences>,
) : AppStorage {

    override suspend fun getString(key: String, default: String?): String? =
        dataStore.data.first()[stringPreferencesKey(key)] ?: default

    override suspend fun putString(key: String, value: String?) {
        dataStore.edit { prefs ->
            if (value == null) prefs.remove(stringPreferencesKey(key))
            else prefs[stringPreferencesKey(key)] = value
        }
    }

    override suspend fun getBoolean(key: String, default: Boolean): Boolean =
        dataStore.data.first()[booleanPreferencesKey(key)] ?: default

    override suspend fun putBoolean(key: String, value: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    override suspend fun getInt(key: String, default: Int): Int =
        dataStore.data.first()[intPreferencesKey(key)] ?: default

    override suspend fun putInt(key: String, value: Int) {
        dataStore.edit { it[intPreferencesKey(key)] = value }
    }

    override suspend fun getLong(key: String, default: Long): Long =
        dataStore.data.first()[longPreferencesKey(key)] ?: default

    override suspend fun putLong(key: String, value: Long) {
        dataStore.edit { it[longPreferencesKey(key)] = value }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
            prefs.remove(booleanPreferencesKey(key))
            prefs.remove(intPreferencesKey(key))
            prefs.remove(longPreferencesKey(key))
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    override fun observeString(key: String): Flow<String?> =
        dataStore.data.map { it[stringPreferencesKey(key)] }
}

/** Android 侧便捷创建（Application 里调一次即可）。 */
fun createAppStorage(context: Context, name: String = "app_storage"): AppStorage =
    DataStoreStorage(
        PreferenceDataStoreFactory.create(
            produceFile = { context.applicationContext.preferencesDataStoreFile(name) }
        )
    )
