package com.example.testdemo.lib.router

/**
 * 导航契约：**只描述「跳到哪」，不描述「怎么跳」**。
 *
 * - Android：`lib:navigation` 用 Navigation Compose 实现
 * - iOS（将来）：换成 Decompose / 原生导航实现
 *
 * 业务页面只依赖本接口，所以底层导航库可替换而业务零改动。
 */
interface Navigator {

    /** 入栈到目标页。 */
    fun navigate(route: AppRoute)

    /** 返回上一页；返回 false 表示已在栈底。 */
    fun navigateUp(): Boolean

    /** 出栈到指定页。[inclusive] 为 true 时该页也一并弹出。 */
    fun popTo(route: AppRoute, inclusive: Boolean = false): Boolean
}
