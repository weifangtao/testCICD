package com.example.testdemo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.testdemo.data.remote.PostDto

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val body: String,
    /** 写入时间，可用于过期判断 / 展示「缓存于 xx」。 */
    val cachedAt: Long,
    /** 收藏状态（数据库 V2 迁移字段示例）。 */
    val isFavorite: Boolean = false,
)

fun PostEntity.toDto() = PostDto(id = id, title = title, body = body)

fun PostDto.toEntity(now: Long = System.currentTimeMillis()) =
    PostEntity(id = id, title = title, body = body, cachedAt = now, isFavorite = false)
