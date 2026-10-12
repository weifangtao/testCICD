package com.example.testdemo.auth

import android.annotation.SuppressLint
import android.provider.Settings
import android.util.Base64
import com.example.testdemo.AppApplication
import com.example.testdemo.BuildConfig
import com.example.testdemo.lib.config.AppEnvironmentConfigHolder
import com.example.testdemo.lib.net.*
import kotlinx.coroutines.CancellationException
import java.util.UUID
import javax.inject.Inject

class AuthRepository @Inject constructor(private val app: AppApplication) {
    @SuppressLint("HardwareIds")
    suspend fun login(account: String, password: String): ApiResult<Unit> = kotlinx.coroutines.withContext<ApiResult<Unit>>(kotlinx.coroutines.Dispatchers.IO) {
        if (account.isBlank() || password.isEmpty()) return@withContext ApiResult.Failure(ApiError.business("请输入账号和密码"))
        val snapshot = app.sessions.state.value
        val environment = AppEnvironmentConfigHolder.current()
        try {
            val encrypted = Base64.encodeToString(PasswordCipher.encryptBytes(password, BuildConfig.WMS_AES_KEY, BuildConfig.WMS_AES_IV), Base64.NO_WRAP)
            val deviceId = app.installationStorage.getString("device_id") ?: run {
                val androidId = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID)
                val id = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") "AID_$androidId" else "RND_${UUID.randomUUID()}"
                app.installationStorage.putString("device_id", id)
                id
            }
            val payload = LoginPayload(account.trim(), encrypted, BuildConfig.VERSION_CODE, deviceId,
                if (app.resources.configuration.locales[0].language == "zh") "CN" else "EN")
            when (val result = app.wms.request<LoginPayload, LoginUser>("AppLogin", payload, authenticated = false)) {
                is ApiResult.Success -> {
                    val user = result.data.copy(userCode = result.data.userCode?.takeIf { it.isNotBlank() } ?: account.trim())
                    if (user.token.isBlank()) ApiResult.Failure(ApiError.business("登录响应缺少 Token"))
                    else if (app.sessions.accept(snapshot.generation, Session(environment.key, user))) ApiResult.Success(Unit)
                    else ApiResult.Cancelled
                }
                is ApiResult.Failure -> if (result.error.type == ApiErrorType.HTTP) ApiResult.Failure(ApiError.service(result.error.code)) else result
                ApiResult.Cancelled -> ApiResult.Cancelled
            }
        } catch (e: CancellationException) { throw e
        } catch (e: IllegalArgumentException) {
            ApiResult.Failure(ApiError.business("登录加密配置不完整，请检查 wms-auth.properties"))
        } catch (_: Exception) {
            ApiResult.Failure(ApiError.business("登录或保存登录信息失败，请重试"))
        }
    }
}
