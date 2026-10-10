package com.example.testdemo.lib.net

import io.ktor.client.engine.HttpClientEngineFactory

/**
 * 平台 HTTP 引擎。
 *
 * - Android → OkHttp
 * - iOS → Darwin（基于 URLSession）
 *
 * Ktor 把差异收敛在这一个函数里，其余网络代码全部跨端共用。
 */
expect fun platformHttpEngine(): HttpClientEngineFactory<*>
