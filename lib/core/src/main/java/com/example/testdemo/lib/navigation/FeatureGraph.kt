package com.example.testdemo.lib.navigation

import androidx.navigation.NavGraphBuilder

/**
 * 一个业务模块的路由图。
 *
 * 各 feature 通过 Hilt 的 `@IntoSet` 贡献实现，**Hilt 自动聚合**：
 * ```
 * @Module
 * @InstallIn(SingletonComponent::class)
 * object HomeGraphModule {
 *     @Provides @IntoSet
 *     fun graph(): FeatureGraph = FeatureGraph { builder ->
 *         builder.composable<HomeRoute> { ... }
 *     }
 * }
 * ```
 *
 * 于是宿主（`MainActivity`）只需要：
 * ```
 * featureGraphs.forEach { it.register(this) }
 * ```
 * **不引用任何具体页面** —— 新增页面时宿主零改动。
 */
fun interface FeatureGraph {
    fun register(builder: NavGraphBuilder)
}
