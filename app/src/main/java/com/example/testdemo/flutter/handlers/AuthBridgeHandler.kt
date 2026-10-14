package com.example.testdemo.flutter.handlers

import com.example.testdemo.auth.SessionManager
import com.example.testdemo.lib.flutter.FlutterBridgeHandler
import com.example.testdemo.lib.flutter.FlutterBridgeResult

/**
 * 供 Flutter 获取与触发 Native 登录态的桥梁处理器。
 */
class GetUserSessionBridgeHandler(
    private val sessionManager: SessionManager,
) : FlutterBridgeHandler {

    override val method: String = "getUserSession"

    override suspend fun handle(arguments: Any?): FlutterBridgeResult<Any?> {
        val session = sessionManager.state.value.session
        if (session == null) {
            return FlutterBridgeResult.Error(
                code = "UNAUTHORIZED",
                message = "当前未登录",
            )
        }

        val sessionMap = mapOf(
            "userCode" to session.user.userCode,
            "userName" to session.user.userName,
            "token" to session.user.token,
            "houseId" to session.user.houseId,
            "houseName" to session.user.houseName,
            "environment" to session.environment,
        )
        return FlutterBridgeResult.Success(sessionMap)
    }
}

/**
 * 供 Flutter 主动触发 Native 退出登录。
 */
class LogoutBridgeHandler(
    private val sessionManager: SessionManager,
) : FlutterBridgeHandler {

    override val method: String = "logout"

    override suspend fun handle(arguments: Any?): FlutterBridgeResult<Any?> {
        sessionManager.logout("Flutter 端主动发起退出登录")
        return FlutterBridgeResult.Success(mapOf("loggedOut" to true))
    }
}
