package com.example.testdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.testdemo.lib.navigation.AppNavHost
import com.example.testdemo.lib.navigation.FeatureGraph
import com.example.testdemo.lib.router.HomeRoute
import com.example.testdemo.ui.theme.TestDemoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 主壳 Activity。
 *
 * ⚠️ **本文件不引用任何具体页面** —— 各 feature 通过 Hilt 的 `@IntoSet`
 * 贡献自己的 [FeatureGraph]，这里只负责遍历注册。
 *
 * ```
 * 新增页面 → 在自己的 feature 里加一个 FeatureGraph，本文件零改动
 * ```
 *
 * 架构说明（混合式）：
 * - **主壳**（本 Activity）：承载绝大多数页面，壳内用 NavHost 导航
 * - **独立壳**（如 `DetailActivity`）：扫码、拍照、WebView 等特殊流程各起一个 Activity，
 *   跨壳跳转用 `startActivity(XxxActivity.intent(...))`
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Hilt 自动聚合所有 feature 贡献的路由图。 */
    @Inject
    lateinit var featureGraphs: Set<@JvmSuppressWildcards FeatureGraph>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TestDemoTheme {
                val navController = rememberNavController()

                AppNavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                ) {
                    // 全部页面在这里自动装配，本文件不需要知道它们是谁
                    featureGraphs.forEach { it.register(this) }
                }
            }
        }
    }
}
