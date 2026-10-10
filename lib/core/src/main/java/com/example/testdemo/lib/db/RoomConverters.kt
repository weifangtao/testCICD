package com.example.testdemo.lib.db

import androidx.room.TypeConverter
import java.math.BigDecimal

/**
 * 通用 Room 类型转换器：把「数据库不原生支持、但业务常用」的类型转成基础类型。
 *
 * 在具体 `@Database` 上声明即可：
 * ```
 * @Database(entities = [...], version = 1)
 * @TypeConverters(AppRoomConverters::class)
 * abstract class AppDatabase : RoomDatabase()
 * ```
 */
object AppRoomConverters {

    private const val LIST_SEPARATOR = "\u001F"

    // ── BigDecimal：金额 / 数量，必须无损（不能用 Double，会丢精度）──
    @TypeConverter
    @JvmStatic
    fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    @JvmStatic
    fun stringToBigDecimal(value: String?): BigDecimal? = value?.toBigDecimalOrNull()

    // ── List<String>：标签、批次号等多值字段 ──
    @TypeConverter
    @JvmStatic
    fun listToString(value: List<String>?): String? =
        value?.takeIf { it.isNotEmpty() }?.joinToString(LIST_SEPARATOR)

    @TypeConverter
    @JvmStatic
    fun stringToList(value: String?): List<String> =
        value?.takeIf { it.isNotEmpty() }?.split(LIST_SEPARATOR).orEmpty()
}
