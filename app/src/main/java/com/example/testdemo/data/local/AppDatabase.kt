package com.example.testdemo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.testdemo.lib.db.AppRoomConverters
import com.example.testdemo.lib.db.buildDatabase

/**
 * 应用数据库。
 *
 * - 构建统一走 `lib.db.buildDatabase`，省掉 builder 样板
 * - 通用类型转换器来自 `lib.db.AppRoomConverters`（BigDecimal / List<String>）
 *
 * 新增实体：加进 `entities`、升 `version`，并补一个显式 Migration。
 * 生产环境禁止 destructive migration，避免静默清空用户数据。
 */
@Database(entities = [PostEntity::class], version = 1, exportSchema = true)
@TypeConverters(AppRoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao

    companion object {
        fun create(context: Context, name: String = "app.db"): AppDatabase =
            buildDatabase<AppDatabase>(context, name)
    }
}
