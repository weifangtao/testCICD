package com.example.testdemo.feature.lab

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.testdemo.lib.navigation.FeatureGraph
import com.example.testdemo.lib.navigation.LocalNavigator
import com.example.testdemo.lib.router.DetailRoute
import com.example.testdemo.lib.router.LabRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

private val LabGraph = FeatureGraph { builder ->
    builder.composable<LabRoute> {
        val navigator = LocalNavigator.current
        val viewModel: LabViewModel = hiltViewModel()

        LabScreen(
            viewModel = viewModel,
            onBack = { navigator.navigateUp() },
            onOpenParamRoute = { id -> navigator.navigate(DetailRoute(id)) },
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object LabGraphModule {
    @Provides
    @IntoSet
    fun provideGraph(): FeatureGraph = LabGraph
}
