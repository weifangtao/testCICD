package com.example.testdemo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    /** 一次性读取（网络失败时回落用）。 */
    @Query("SELECT * FROM posts ORDER BY id")
    suspend fun getAll(): List<PostEntity>

    /** 响应式读取：数据变化自动推送，适合「数据库为单一数据源」。 */
    @Query("SELECT * FROM posts ORDER BY id")
    fun observeAll(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(posts: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun clear()

    /** 用于全量同步，避免服务端删除记录后本地永久残留。 */
    @Transaction
    suspend fun replaceAll(posts: List<PostEntity>) {
        clear()
        upsertAll(posts)
    }
}
