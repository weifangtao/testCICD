package com.example.testdemo.lib.navigation

import androidx.navigation.NavHostController
import com.example.testdemo.lib.router.AppRoute
import com.example.testdemo.lib.router.Navigator

/**
 * 用 Navigation Compose 实现 [Navigator]。
 *
 * 路由对象是 `@Serializable` 的，所以 `navigate(route)` 走的是
 * 类型安全重载，参数在编译期校验。
 */
class NavigationNavigator(
    private val navController: NavHostController,
) : Navigator {

    override fun navigate(route: AppRoute) {
        navController.navigate(route)
    }

    override fun navigateUp(): Boolean = navController.navigateUp()

    override fun popTo(route: AppRoute, inclusive: Boolean): Boolean =
        navController.popBackStack(route, inclusive)
}
