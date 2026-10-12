package com.example.testdemo.di

import android.content.Context
import com.example.testdemo.AppApplication
import com.example.testdemo.data.PostRepository
import com.example.testdemo.data.local.PostDao
import com.example.testdemo.lib.storage.AppStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用级依赖。
 *
 * 目前复用 [AppApplication] 作为组装点；将来依赖变多可以在这里逐步接管。
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideApp(@ApplicationContext context: Context): AppApplication =
        context as AppApplication

    @Provides
    fun provideStorage(app: AppApplication): AppStorage = app.storage

    @Provides
    fun providePostDao(app: AppApplication): PostDao = app.database.postDao()

    @Provides
    fun providePostRepository(app: AppApplication): PostRepository =
        PostRepository(
            // 公开客户端与固定会话 DAO 配套，避免跨账号写入
            networkProvider = { app.network },
            postDao = app.database.postDao().let { dao -> { dao } },
        )
}
