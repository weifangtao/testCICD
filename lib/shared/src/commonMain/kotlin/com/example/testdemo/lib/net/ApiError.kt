package com.example.testdemo.lib.net

/** 网络错误分类，便于上层按类型做差异化处理（重试 / 提示 / 上报）。 */
enum class ApiErrorType {
    BUSINESS, EMPTY_BODY, HTTP, TIMEOUT, PARSE, CONNECT, UNKNOWN
}

/** 统一的网络 / 业务错误模型。移植自 k3_wms_android 的 NetworkError。 */
data class ApiError(
    val type: ApiErrorType,
    val message: String,
    val code: String? = null,
) {
    companion object {
        fun connect() = ApiError(ApiErrorType.CONNECT, "网络连接失败，请稍后重试")
        fun timeout() = ApiError(ApiErrorType.TIMEOUT, "网络连接超时，请稍后重试")
        fun parse() = ApiError(ApiErrorType.PARSE, "数据解析错误，请稍后重试")
        fun emptyBody() = ApiError(ApiErrorType.EMPTY_BODY, "服务器返回数据为空，请稍后重试")

        fun service(code: String? = null) =
            ApiError(ApiErrorType.HTTP, "服务器不稳定，请稍后重试", code)

        fun unknown(message: String? = null) = ApiError(
            ApiErrorType.UNKNOWN,
            message?.takeIf { it.isNotBlank() } ?: "未知错误，请稍后重试",
        )

        fun business(message: String?, code: String? = null) = ApiError(
            ApiErrorType.BUSINESS,
            message?.takeIf { it.isNotBlank() } ?: "业务处理失败",
            code,
        )
    }
}
