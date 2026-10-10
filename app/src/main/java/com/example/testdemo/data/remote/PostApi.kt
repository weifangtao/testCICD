package com.example.testdemo.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class PostDto(
    val id: Int,
    val title: String,
    val body: String,
)

/** 接口路径常量（Ktor 不需要 Retrofit 接口声明）。 */
object PostApi {
    const val POSTS = "posts"
}
