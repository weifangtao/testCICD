package com.example.testdemo.feature.detail

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.testdemo.lib.base.compose.MviScreen
import com.example.testdemo.lib.navigation.FeatureGraph
import com.example.testdemo.lib.navigation.LocalNavigator
import com.example.testdemo.lib.router.DetailRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * 壳内版本的详情页（`DetailActivity` 是独立壳版本，两者共用 [DetailContent]）。
 *
 * 参数从 NavBackStackEntry 的 SavedStateHandle 取，所以和 Activity 版共用同一个 ViewModel。
 */
private val DetailGraph = FeatureGraph { builder ->
    builder.composable<DetailRoute> { entry ->
        val navigator = LocalNavigator.current
        val viewModel: DetailViewModel = hiltViewModel()

        MviScreen(viewModel = viewModel, onEvent = {}) { state ->
            DetailContent(
                id = state.id,
                favorite = state.favorite,
                sourceLabel = "壳内 Compose 页面（NavHost）",
                onToggleFavorite = { viewModel.dispatch(DetailIntent.ToggleFavorite) },
                onBack = { navigator.navigateUp() },
            )
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DetailGraphModule {
    @Provides
    @IntoSet
    fun provideGraph(): FeatureGraph = DetailGraph
}
