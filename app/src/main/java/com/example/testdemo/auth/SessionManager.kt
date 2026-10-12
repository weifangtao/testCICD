package com.example.testdemo.auth

import com.example.testdemo.lib.config.AppEnvironment
import com.example.testdemo.lib.config.AppEnvironmentConfigHolder
import com.example.testdemo.lib.storage.AppStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** 所有登录态写入串行化；旧请求只允许改变发起时的会话。 */
class SessionManager(
    private val storage: AppStorage,
    private val encrypt: (String) -> String,
    private val decrypt: (String) -> String,
) {
    private val mutableState = MutableStateFlow(AuthState())
    val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun restore() = mutex.withLock {
        if (state.value.ready) return@withLock
        val env = AppEnvironmentConfigHolder.current().key
        var message: String? = null
        val session = try {
            storage.getString("session")?.let { json.decodeFromString<Session>(decrypt(it)) }
                ?.takeIf { it.environment == env && it.user.token.isNotBlank() && !it.user.userCode.isNullOrBlank() }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) {
            message = "登录信息无法恢复，请重新登录"
            null
        }
        mutableState.value = AuthState(ready = true, session = session, message = message)
    }

    suspend fun accept(expected: Long, session: Session): Boolean = mutex.withLock {
        if (state.value.generation != expected || session.environment != AppEnvironmentConfigHolder.current().key) return@withLock false
        require(session.user.token.isNotBlank() && !session.user.userCode.isNullOrBlank())
        storage.putString("session", encrypt(json.encodeToString(Session.serializer(), session)))
        mutableState.value = AuthState(true, session, expected + 1)
        true
    }

    /**
     * 无感刷新：更新当前会话的 Token 凭据，保持 generation 不变以继续后续业务请求。
     */
    suspend fun updateToken(newToken: String): Boolean = mutex.withLock {
        val currentSession = state.value.session ?: return@withLock false
        if (newToken.isBlank()) return@withLock false
        val updatedUser = currentSession.user.copy(token = newToken)
        val updatedSession = currentSession.copy(user = updatedUser)
        storage.putString("session", encrypt(json.encodeToString(Session.serializer(), updatedSession)))
        mutableState.value = state.value.copy(session = updatedSession)
        true
    }

    suspend fun logout(message: String? = null, expected: Long? = null) = mutex.withLock {
        if (expected != null && expected != state.value.generation) return@withLock
        storage.remove("session")
        mutableState.value = AuthState(true, generation = state.value.generation + 1, message = message)
    }

    suspend fun switchEnvironment(target: AppEnvironment) = mutex.withLock {
        if (target == AppEnvironmentConfigHolder.current()) return@withLock
        storage.remove("session")
        AppEnvironmentConfigHolder.switchTo(target)
        mutableState.value = AuthState(true, generation = state.value.generation + 1, message = "已切换环境，请登录")
    }
}
