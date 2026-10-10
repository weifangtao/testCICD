package com.example.testdemo.lib.image.engine

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.testdemo.lib.image.ImageEngine
import com.example.testdemo.lib.image.ImageOptions

/**
 * 默认图片引擎：基于 Coil 实现。
 *
 * 之所以把默认实现放这里，是因为 Compose 生态里 Coil 的接入成本最低；
 * k3_wms_android 用的是 Glide（ImageView 体系），如需对齐只需另写一个
 * `ImageEngine` 实现并 `ImageLoader.init(...)`，业务代码零改动。
 */
class DefaultImageEngine : ImageEngine {

    @Composable
    override fun Image(
        model: Any?,
        modifier: Modifier,
        options: ImageOptions,
        contentScale: ContentScale,
        contentDescription: String?,
    ) {
        val context = LocalContext.current
        AsyncImage(
            model = buildRequest(context, model, options),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            placeholder = if (options.hasPlaceholder()) painterResource(options.placeholderResId) else null,
            error = if (options.hasError()) painterResource(options.errorResId) else null,
        )
    }

    override suspend fun preload(context: Context, model: Any?, options: ImageOptions) {
        context.imageLoader.execute(buildRequest(context, model, options))
    }

    override fun clearMemory(context: Context) {
        context.imageLoader.memoryCache?.clear()
    }

    @OptIn(ExperimentalCoilApi::class)
    override fun clearDisk(context: Context) {
        context.imageLoader.diskCache?.clear()
    }

    private fun buildRequest(context: Context, model: Any?, options: ImageOptions): ImageRequest =
        ImageRequest.Builder(context)
            .data(model)
            .memoryCachePolicy(
                if (options.isSkipMemoryCache) CachePolicy.DISABLED else CachePolicy.ENABLED
            )
            .diskCachePolicy(
                if (options.isDiskCacheEnabled) CachePolicy.ENABLED else CachePolicy.DISABLED
            )
            .crossfade(true)
            .build()
}
