package com.example.testdemo.data

import com.example.testdemo.data.local.PostDao
import com.example.testdemo.data.local.toDto
import com.example.testdemo.data.local.toEntity
import com.example.testdemo.data.remote.PostApi
import com.example.testdemo.data.remote.PostDto
import com.example.testdemo.lib.net.ApiResult
import com.example.testdemo.lib.net.NetworkClient
import com.example.testdemo.lib.net.getResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 仓库层：**Room 是单一数据源（SSOT）**。
 *
 * - [observePosts]：UI 订阅它拿数据
 * - [refresh]：只负责「拉网络 → 写库」
 *
 * 此实现依赖 Android Room；iOS 复用共享网络契约时需提供自己的持久化实现。
 */
class PostRepository(
    private val networkProvider: () -> NetworkClient,
    private val postDao: () -> PostDao,
) {
    private val refreshMutex = Mutex()

    fun observePosts(): Flow<List<PostDto>> =
        postDao().observeAll().map { rows -> rows.map { it.toDto() } }

    suspend fun refresh(): ApiResult<Unit> = refreshMutex.withLock {
        runCatching {
            when (val result = networkProvider().getResult<List<PostDto>>(PostApi.POSTS)) {
                is ApiResult.Success -> {
                    postDao().replaceAll(result.data.map { it.toEntity() })
                    ApiResult.Success(Unit)
                }

                is ApiResult.Failure -> result
                ApiResult.Cancelled -> ApiResult.Cancelled
            }
        }.getOrElse { throwable ->
            if (throwable is kotlinx.coroutines.CancellationException) throw throwable
            ApiResult.Failure(com.example.testdemo.lib.net.ErrorMapper.fromThrowable(throwable))
        }
    }
}
