package com.example.testdemo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.testdemo.lib.db.AppRoomConverters
import com.example.testdemo.lib.db.buildDatabase

/**
 * 应用数据库。
 *
 * - 构建统一走 `lib.db.buildDatabase`
 * - 声明显式 Migration 进行平滑升级
 * - 生产环境禁止 destructive migration，避免静默清空用户数据
 */
@Database(entities = [PostEntity::class], version = 2, exportSchema = true)
@TypeConverters(AppRoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE posts ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun create(context: Context, name: String = "app.db"): AppDatabase =
            buildDatabase<AppDatabase>(context, name) {
                addMigrations(MIGRATION_1_2)
            }
    }
}
