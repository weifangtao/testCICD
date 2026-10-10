package com.example.testdemo.lib.image

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.example.testdemo.lib.image.engine.DefaultImageEngine

/**
 * 图片加载门面（外观模式的"调用侧"）。
 *
 * 业务统一走这里，因此底层换 Coil / Glide / 自研都不影响业务代码：
 * ```
 * AppImage(model = url, modifier = Modifier.size(48.dp))
 *
 * // 换引擎（可选，默认内置实现）
 * ImageLoader.init(MyGlideEngine())
 * ```
 */
object ImageLoader {

    @Volatile
    private var engine: ImageEngine = DefaultImageEngine()

    /** 在 Application 中按需替换底层实现。 */
    fun init(engine: ImageEngine) {
        this.engine = engine
    }

    @Composable
    fun Image(
        model: Any?,
        modifier: Modifier = Modifier,
        options: ImageOptions = ImageOptions.none(),
        contentScale: ContentScale = ContentScale.Crop,
        contentDescription: String? = null,
    ) {
        engine.Image(
            model = model,
            modifier = modifier,
            options = options,
            contentScale = contentScale,
            contentDescription = contentDescription,
        )
    }

    suspend fun preload(context: Context, model: Any?, options: ImageOptions = ImageOptions.none()) {
        engine.preload(context, model, options)
    }

    fun clearMemory(context: Context) = engine.clearMemory(context)

    fun clearDisk(context: Context) = engine.clearDisk(context)
}

/** 业务里写 `AppImage(...)` 比 `ImageLoader.Image(...)` 更顺手。 */
@Composable
fun AppImage(
    model: Any?,
    modifier: Modifier = Modifier,
    options: ImageOptions = ImageOptions.none(),
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
) {
    ImageLoader.Image(model, modifier, options, contentScale, contentDescription)
}
