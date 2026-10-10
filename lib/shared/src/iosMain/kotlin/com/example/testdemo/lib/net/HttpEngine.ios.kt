package com.example.testdemo.lib.net

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpEngine(): HttpClientEngineFactory<*> = Darwin
