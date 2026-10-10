package com.example.testdemo.lib.router

import kotlinx.serialization.Serializable

/**
 * 路由标记。
 *
 * 实现必须是 `@Serializable` 的 `data object` 或 `data class` ——
 * 这样 Navigation 能做**类型安全路由**，参数在编译期校验、
 * 进程被杀后也能从 back stack 恢复。
 *
 * 本模块刻意**不依赖任何平台与导航库**：
 * 将来 iOS 端换 Decompose 或原生导航时，路由表可以原样复用。
 */
interface AppRoute

/** 首页。 */
@Serializable
data object HomeRoute : AppRoute

/** 详情页，带参数。 */
@Serializable
data class DetailRoute(val id: String) : AppRoute

/** 设置页。 */
@Serializable
data object SettingsRoute : AppRoute

/** 能力演示页。 */
@Serializable
data object LabRoute : AppRoute
