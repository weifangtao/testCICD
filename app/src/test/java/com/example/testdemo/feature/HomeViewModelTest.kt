package com.example.testdemo.feature

import com.example.testdemo.data.PostRepository
import com.example.testdemo.data.remote.PostDto
import com.example.testdemo.feature.home.HomeEvent
import com.example.testdemo.feature.home.HomeIntent
import com.example.testdemo.feature.home.HomeState
import com.example.testdemo.feature.home.HomeViewModel
import com.example.testdemo.lib.mvi_testing.BaseMviViewModelTest
import com.example.testdemo.lib.mvi_testing.expectThat
import com.example.testdemo.lib.mvi_testing.returnsWithDelay
import com.example.testdemo.lib.net.ApiError
import com.example.testdemo.lib.net.ApiResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val POSTS = listOf(PostDto(1, "标题", "内容"))

/**
 * 单一数据源（SSOT）行为验证。
 *
 * 状态序列固定为 3 帧：
 * ```
 * ① 数据库首帧（posts 已就绪，不闪空列表）
 * ② 刷新中（isLoading = true）
 * ③ 刷新结束（loaded = true；失败则带 error）
 * ```
 * 因为 `observePosts()` 是同步发射的，`stateIn` 的初始空值会被 ① 立即覆盖。
 */
class HomeViewModelTest :
    BaseMviViewModelTest<HomeIntent, HomeState, HomeEvent, HomeViewModel>() {

    private val repository = mockk<PostRepository>()

    @Test
    fun `database content is pushed into state and refresh completes`() {
        every { repository.observePosts() } returns flowOf(POSTS)
        coEvery { repository.refresh() } returnsWithDelay ApiResult.Success(Unit)

        runVMTest(
            vmProducer = { HomeViewModel(repository) },
            intents = flowOf(HomeIntent.Load),
            expectedStates = listOf(
                expectThat { assertEquals("① 数据库首帧", POSTS, it.posts) },
                expectThat { assertTrue("② 刷新中", it.isLoading) },
                expectThat {
                    assertEquals("③ 数据仍在", POSTS, it.posts)
                    assertTrue("刷新完成", it.loaded)
                    assertNull("成功时无错误", it.error)
                },
            ),
        )
    }

    @Test
    fun `refresh failure keeps cached posts and flags offline`() {
        every { repository.observePosts() } returns flowOf(POSTS)
        coEvery { repository.refresh() } returnsWithDelay ApiResult.Failure(ApiError.connect())

        runVMTest(
            vmProducer = { HomeViewModel(repository) },
            intents = flowOf(HomeIntent.Load),
            expectedStates = listOf(
                expectThat { assertEquals(POSTS, it.posts) },
                expectThat { assertTrue(it.isLoading) },
                expectThat {
                    assertEquals("失败后缓存不能丢", POSTS, it.posts)
                    assertEquals(ApiError.connect().message, it.error)
                    assertTrue("有缓存 + 刷新失败 = 离线模式", it.isOfflineWithCache)
                },
            ),
            expectedEvents = listOf(
                expectThat { assertTrue(it is HomeEvent.ShowMessage) },
            ),
        )
    }

    /**
     * 库为空时，combine 首帧与 `stateIn` 初始值相等 → StateFlow 值不变，
     * 收集器首个状态就是「初始空态」。
     */
    @Test
    fun `refresh failure with empty database surfaces error instead of offline`() {
        every { repository.observePosts() } returns flowOf(emptyList())
        coEvery { repository.refresh() } returnsWithDelay ApiResult.Failure(ApiError.timeout())

        runVMTest(
            vmProducer = { HomeViewModel(repository) },
            intents = flowOf(HomeIntent.Load),
            expectedStates = listOf(
                expectThat {
                    assertTrue("① 初始空态", it.posts.isEmpty())
                    assertFalse(it.isLoading)
                },
                expectThat { assertTrue("② 刷新中", it.isLoading) },
                expectThat {
                    assertTrue("③ 库仍为空", it.posts.isEmpty())
                    assertFalse("库里没数据时不应算离线缓存", it.isOfflineWithCache)
                    assertEquals(ApiError.timeout().message, it.error)
                },
            ),
            expectedEvents = listOf(
                expectThat { assertTrue(it is HomeEvent.ShowMessage) },
            ),
        )
    }

    @Test
    fun `Refresh intent behaves like Load`() {
        every { repository.observePosts() } returns flowOf(POSTS)
        coEvery { repository.refresh() } returnsWithDelay ApiResult.Success(Unit)

        runVMTest(
            vmProducer = { HomeViewModel(repository) },
            intents = flowOf(HomeIntent.Refresh),
            expectedStates = listOf(
                expectThat { assertEquals(POSTS, it.posts) },
                expectThat { assertTrue(it.isLoading) },
                expectThat { assertTrue(it.loaded) },
            ),
        )
    }
}
