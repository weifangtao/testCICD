package com.example.testdemo.lib.image

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * 图片引擎抽象（外观模式的"实现侧"）。
 *
 * 业务永远不直接依赖 Glide / Coil，只依赖 [ImageLoader]；
 * 换框架时实现这个接口并用 [ImageLoader.init] 注入即可。
 */
interface ImageEngine {

    /** Compose 渲染入口。 */
    @Composable
    fun Image(
        model: Any?,
        modifier: Modifier,
        options: ImageOptions,
        contentScale: ContentScale,
        contentDescription: String?,
    )

    /** 预加载到缓存（列表预取、详情页提前加载）。 */
    suspend fun preload(context: Context, model: Any?, options: ImageOptions)

    fun clearMemory(context: Context)

    fun clearDisk(context: Context)
}
