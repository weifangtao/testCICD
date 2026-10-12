package com.example.testdemo.domain.usecase

import com.example.testdemo.auth.AuthRepository
import com.example.testdemo.lib.domain.SuspendUseCase
import com.example.testdemo.lib.net.ApiResult
import javax.inject.Inject

data class LoginParams(val account: String, val password: String)

/**
 * 账号登录用例：入参校验、密文加密、网络校验及会话保存。
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) : SuspendUseCase<LoginParams, ApiResult<Unit>> {
    override suspend fun invoke(params: LoginParams): ApiResult<Unit> =
        authRepository.login(params.account, params.password)
}
