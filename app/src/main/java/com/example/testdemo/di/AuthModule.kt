package com.example.testdemo.di

import com.example.testdemo.AppApplication
import com.example.testdemo.auth.SessionManager
import com.example.testdemo.auth.WmsGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides @Singleton fun provideSessionManager(app: AppApplication): SessionManager = app.sessions
    @Provides @Singleton fun provideWmsGateway(app: AppApplication): WmsGateway = app.wms
}
