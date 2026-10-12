package com.example.testdemo.auth

import com.example.testdemo.lib.config.AppEnvironmentConfigHolder
import com.example.testdemo.lib.net.*

/** WMS 请求统一入口。客户端绑定请求发起时的 Token，不共享到公开示例接口。 */
class WmsGateway(
    val sessions: SessionManager,
    val clientFactory: (NetworkConfig) -> NetworkClient = { NetworkClient(it) },
) {
    suspend inline fun <reified B : Any, reified T> request(
        function: String, body: B, authenticated: Boolean = true,
    ): ApiResult<T> {
        val snapshot = sessions.state.value
        val environment = AppEnvironmentConfigHolder.current()
        if (authenticated && snapshot.session == null) return ApiResult.Failure(ApiError.business("请先登录"))
        val token = snapshot.session?.takeIf { it.environment == environment.key }?.user?.token
        val client = clientFactory(NetworkConfig(
            baseUrl = environment.baseUrl,
            // 登录凭据及业务报文不进入 HTTP 日志。
            headerProvider = { if (authenticated && token != null) mapOf("SESSION_MOBILE_USER_TOKEN" to token) else emptyMap() },
        ))
        return try {
            val result = client.postResult<WmsRequest<B>, ApiEnvelope<T>>("app", WmsRequest(function, body)).unwrapEnvelope()
            if (authenticated && result is ApiResult.Failure &&
                (result.error.code == "401" || result.error.message == "请重新登录")) {
                sessions.logout("登录已失效，请重新登录", snapshot.generation)
            }
            result
        } finally { client.close() }
    }
}
