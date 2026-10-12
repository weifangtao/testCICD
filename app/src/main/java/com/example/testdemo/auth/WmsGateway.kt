package com.example.testdemo.auth

import com.example.testdemo.lib.config.AppEnvironmentConfigHolder
import com.example.testdemo.lib.net.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** WMS 请求统一入口。具备 401 无感刷新与并发锁防冲刷机制。 */
class WmsGateway(
    val sessions: SessionManager,
    val refreshTokenProvider: (suspend (Session) -> ApiResult<String>)? = null,
    val clientFactory: (NetworkConfig) -> NetworkClient = { NetworkClient(it) },
) {
    val tokenRefreshMutex = Mutex()

    suspend inline fun <reified B : Any, reified T> request(
        function: String, body: B, authenticated: Boolean = true,
    ): ApiResult<T> {
        val snapshot = sessions.state.value
        val environment = AppEnvironmentConfigHolder.current()
        if (authenticated && snapshot.session == null) return ApiResult.Failure(ApiError.business("请先登录"))

        var currentToken = snapshot.session?.takeIf { it.environment == environment.key }?.user?.token
        var client = createClient(environment.baseUrl, authenticated, currentToken)

        return try {
            var result = client.postResult<WmsRequest<B>, ApiEnvelope<T>>("app", WmsRequest(function, body)).unwrapEnvelope()

            if (authenticated && result is ApiResult.Failure && isTokenExpiredError(result.error)) {
                val refreshSuccess = attemptTokenRefresh(currentToken)
                if (refreshSuccess) {
                    client.close()
                    currentToken = sessions.state.value.session?.user?.token
                    client = createClient(environment.baseUrl, authenticated, currentToken)
                    result = client.postResult<WmsRequest<B>, ApiEnvelope<T>>("app", WmsRequest(function, body)).unwrapEnvelope()
                } else {
                    sessions.logout("登录已失效，请重新登录", snapshot.generation)
                }
            }
            result
        } finally {
            client.close()
        }
    }

    fun createClient(baseUrl: String, authenticated: Boolean, token: String?): NetworkClient =
        clientFactory(
            NetworkConfig(
                baseUrl = baseUrl,
                headerProvider = { if (authenticated && token != null) mapOf("SESSION_MOBILE_USER_TOKEN" to token) else emptyMap() },
            )
        )

    fun isTokenExpiredError(error: ApiError): Boolean =
        error.code == "401" || error.message == "请重新登录" || error.message.contains("Token已过期")

    suspend fun attemptTokenRefresh(currentTokenAtFailure: String?): Boolean = tokenRefreshMutex.withLock {
        val latestSession = sessions.state.value.session ?: return false
        val latestToken = latestSession.user.token

        if (latestToken != currentTokenAtFailure && latestToken.isNotBlank()) {
            return true
        }

        val provider = refreshTokenProvider ?: { session ->
            val client = createClient(session.environment, true, session.user.token)
            try {
                client.postResult<WmsRequest<String>, ApiEnvelope<String>>(
                    "app", WmsRequest("AppRefreshToken", session.user.token)
                ).unwrapEnvelope()
            } finally {
                client.close()
            }
        }

        return try {
            when (val refreshResult = provider(latestSession)) {
                is ApiResult.Success -> {
                    if (refreshResult.data.isNotBlank()) {
                        sessions.updateToken(refreshResult.data)
                        true
                    } else false
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }
}
