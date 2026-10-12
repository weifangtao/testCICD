package com.example.testdemo.lib.net

/**
 * 服务端统一响应体外层。
 *
 * 字段名按公司后端约定；如果后端返回结构不同，在业务层定义对应的可序列化 DTO。
 */
@kotlinx.serialization.Serializable
data class ApiEnvelope<T>(
    val result: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val code: String? = null,
)
