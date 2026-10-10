package com.example.testdemo.lib.net

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual fun platformHttpEngine(): HttpClientEngineFactory<*> = OkHttp
