package com.example.testdemo.auth

import androidx.lifecycle.viewModelScope
import com.example.testdemo.BuildConfig
import com.example.testdemo.lib.base.mvi.*
import com.example.testdemo.lib.config.AppEnvironment
import com.example.testdemo.lib.net.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(val loading: Boolean = false, val message: String? = null) : MviViewState
sealed interface LoginIntent : MviIntent {
    class Submit(val account: String, val password: String) : LoginIntent {
        override fun toString() = "LoginIntent.Submit(<redacted>)"
    }
    data class Environment(val value: AppEnvironment) : LoginIntent
}
sealed interface LoginEvent : MviSingleEvent

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessions: SessionManager,
) : AbstractMviViewModel<LoginIntent, LoginUiState, LoginEvent>() {
    private val mutableState = MutableStateFlow(LoginUiState())
    override val viewState = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            intents.collect { intent ->
                // 防止连点导致多个登录请求排队；密码不写入状态、日志或持久化。
                if (mutableState.value.loading) return@collect
                mutableState.value = LoginUiState(loading = true)
                launch {
                    try {
                        when (intent) {
                            is LoginIntent.Submit -> when (val result = repository.login(intent.account, intent.password)) {
                                is ApiResult.Failure -> mutableState.value = LoginUiState(message = result.error.message)
                                else -> mutableState.value = LoginUiState()
                            }
                            is LoginIntent.Environment -> {
                                if (BuildConfig.DEBUG) sessions.switchEnvironment(intent.value)
                                mutableState.value = LoginUiState()
                            }
                        }
                    } catch (e: CancellationException) { throw e
                    } catch (_: Exception) {
                        mutableState.value = LoginUiState(message = "操作失败，请重试")
                    } finally {
                        mutableState.value = mutableState.value.copy(loading = false)
                    }
                }
            }
        }
    }
}
