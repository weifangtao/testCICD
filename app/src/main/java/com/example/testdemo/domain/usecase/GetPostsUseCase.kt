package com.example.testdemo.domain.usecase

import com.example.testdemo.data.PostRepository
import com.example.testdemo.data.remote.PostDto
import com.example.testdemo.lib.domain.NoParamsFlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 订阅文章列表用例（单一数据源 Flow）。
 */
class GetPostsUseCase @Inject constructor(
    private val repository: PostRepository,
) : NoParamsFlowUseCase<List<PostDto>> {
    override fun invoke(): Flow<List<PostDto>> = repository.observePosts()
}
