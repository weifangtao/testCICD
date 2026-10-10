package com.example.testdemo.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.testdemo.di.rememberAppStorage
import com.example.testdemo.lib.navigation.FeatureGraph
import com.example.testdemo.lib.navigation.LocalNavigator
import com.example.testdemo.lib.router.LabRoute
import com.example.testdemo.lib.router.SettingsRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

private val SettingsGraph = FeatureGraph { builder ->
    builder.composable<SettingsRoute> {
        val navigator = LocalNavigator.current

        SettingsScreen(
            storage = rememberAppStorage(),
            onBack = { navigator.navigateUp() },
            onOpenLab = { navigator.navigate(LabRoute) },
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object SettingsGraphModule {
    @Provides
    @IntoSet
    fun provideGraph(): FeatureGraph = SettingsGraph
}
