package com.example.testdemo.lib.router

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 路由契约测试。
 *
 * Navigation 的类型安全路由依赖 kotlinx-serialization 存取参数，
 * 所以「每个路由都能无损序列化」是硬性前提 —— 新增路由忘了加
 * `@Serializable` 会在这里被拦住。
 */
class AppRouteTest {

    @Test
    fun `all routes survive serialization round trip`() {
        assertRoundTrip(HomeRoute)
        assertRoundTrip(SettingsRoute)
        assertRoundTrip(LabRoute)
        assertRoundTrip(DetailRoute("42"))
        assertRoundTrip(DetailRoute(""))          // 边界：空参数
        assertRoundTrip(DetailRoute("a/b?c=d"))   // 边界：含 URL 特殊字符
    }

    @Test
    fun `detail route equality is based on id`() {
        assertEquals(DetailRoute("42"), DetailRoute("42"))
        assertNotEquals(DetailRoute("42"), DetailRoute("43"))
    }

    @Test
    fun `different routes are never equal`() {
        val routes: List<AppRoute> = listOf(
            HomeRoute,
            SettingsRoute,
            LabRoute,
            DetailRoute("1"),
            DetailRoute("2"),
        )
        assertEquals("路由之间应互不相等", routes.size, routes.toSet().size)
    }

    private inline fun <reified T : AppRoute> assertRoundTrip(route: T) {
        val encoded = Json.encodeToString(route)
        val decoded = Json.decodeFromString<T>(encoded)
        assertEquals("路由序列化后应还原：$encoded", route, decoded)
    }
}
