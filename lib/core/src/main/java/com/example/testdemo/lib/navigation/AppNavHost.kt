package com.example.testdemo.lib.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.testdemo.lib.router.AppRoute
import com.example.testdemo.lib.router.Navigator

/** 业务页面通过它拿导航能力，不必层层透传 navController。 */
val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator 未提供：请在页面外层使用 AppNavHost 或 ProvideNavigator")
}

/** 把 [NavHostController] 包装成平台无关的 [Navigator] 并下发。 */
@Composable
fun ProvideNavigator(
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val navigator = remember(navController) { NavigationNavigator(navController) }
    CompositionLocalProvider(LocalNavigator provides navigator, content = content)
}

/**
 * 统一导航宿主：`NavHost` + `ProvideNavigator` 一次配好。
 *
 * ```
 * AppNavHost(navController, startDestination = HomeRoute) {
 *     composable<HomeRoute> { HomeScreen(...) }
 *     composable<DetailRoute> { entry ->
 *         DetailScreen(id = entry.toRoute<DetailRoute>().id)
 *     }
 * }
 * ```
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: AppRoute,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    ProvideNavigator(navController) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier,
            builder = builder,
        )
    }
}
