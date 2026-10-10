package com.example.testdemo.lib.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 统一的 Room 数据库构建入口，省掉每个项目重复写 builder 样板。
 *
 * ```
 * @Database(entities = [PostEntity::class], version = 1, exportSchema = true)
 * @TypeConverters(AppRoomConverters::class)
 * abstract class AppDatabase : RoomDatabase() {
 *     abstract fun postDao(): PostDao
 *
 *     companion object {
 *         fun create(context: Context) = buildDatabase<AppDatabase>(context, "app.db") {
 *             // 生产环境请使用显式 addMigrations(...)，禁止 destructive migration
 *         }
 *     }
 * }
 * ```
 */
inline fun <reified T : RoomDatabase> buildDatabase(
    context: Context,
    name: String,
    builder: RoomDatabase.Builder<T>.() -> Unit = {},
): T = Room.databaseBuilder(context.applicationContext, T::class.java, name)
    .apply(builder)
    .build()
