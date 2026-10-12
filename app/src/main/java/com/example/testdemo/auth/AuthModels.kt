package com.example.testdemo.auth

import kotlinx.serialization.Serializable
import java.security.MessageDigest

@Serializable
data class WmsRequest<T>(val functionCode: String, val data: T)

@Serializable
class LoginPayload(
    val userCode: String,
    val password: String,
    val appVersion: Int,
    val imei: String,
    val language: String,
    val employmentType: Boolean = false,
)

@Serializable
data class LoginUser(
    val token: String,
    val userCode: String? = null,
    val userName: String? = null,
    val houseId: String? = null,
    val houseName: String? = null,
) {
    override fun toString() = "LoginUser(<redacted>)"
}

@Serializable
data class Session(val environment: String, val user: LoginUser) {
    val scopeKey: String get() = environment + "_" + MessageDigest.getInstance("SHA-256")
        .digest("${user.userCode}|${user.houseId.orEmpty()}".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    override fun toString() = "Session(environment=$environment)"
}

data class AuthState(
    val ready: Boolean = false,
    val session: Session? = null,
    val generation: Long = 0,
    val message: String? = null,
)
