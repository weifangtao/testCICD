package com.example.testdemo.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.testdemo.lib.storage.AppStorage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * 纯展示页面（没有 ViewModel）在 Composable 里取依赖的入口。
 *
 * 避免为了拿一个 `AppStorage` 硬造一个 ViewModel。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun appStorage(): AppStorage
}

@Composable
fun rememberAppStorage(): AppStorage {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppEntryPoint::class.java,
        ).appStorage()
    }
}
