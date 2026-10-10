package com.example.testdemo.feature.home

import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.testdemo.feature.detail.DetailActivity
import com.example.testdemo.lib.navigation.FeatureGraph
import com.example.testdemo.lib.navigation.LocalNavigator
import com.example.testdemo.lib.router.HomeRoute
import com.example.testdemo.lib.router.SettingsRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** 首页路由图 —— feature 自己拥有，宿主不需要知道它。 */
private val HomeGraph = FeatureGraph { builder ->
    builder.composable<HomeRoute> {
        val navigator = LocalNavigator.current
        val context = LocalContext.current
        val viewModel: HomeViewModel = hiltViewModel()

        HomeScreen(
            viewModel = viewModel,
            // 跨壳：另起一个 Activity（见 DetailActivity）
            onOpenDetail = { id ->
                context.startActivity(DetailActivity.intent(context, id.toString()))
            },
            // 壳内：交给 NavHost
            onOpenSettings = { navigator.navigate(SettingsRoute) },
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object HomeGraphModule {
    @Provides
    @IntoSet
    fun provideGraph(): FeatureGraph = HomeGraph
}
