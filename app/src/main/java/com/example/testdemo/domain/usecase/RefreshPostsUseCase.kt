package com.example.testdemo.domain.usecase

import com.example.testdemo.data.PostRepository
import com.example.testdemo.lib.domain.NoParamsSuspendUseCase
import com.example.testdemo.lib.net.ApiResult
import javax.inject.Inject

/**
 * 刷新文章列表用例（网络请求并同步写库）。
 */
class RefreshPostsUseCase @Inject constructor(
    private val repository: PostRepository,
) : NoParamsSuspendUseCase<ApiResult<Unit>> {
    override suspend fun invoke(): ApiResult<Unit> = repository.refresh()
}
