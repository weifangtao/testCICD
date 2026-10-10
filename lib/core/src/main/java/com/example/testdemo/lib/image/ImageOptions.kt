package com.example.testdemo.lib.image

/**
 * 图片加载选项：与具体图片框架无关，业务只依赖它。
 *
 * 移植自 k3_wms_android 的 ImageOptions，保持同一套 Builder API。
 */
class ImageOptions private constructor(builder: Builder) {

    val placeholderResId: Int = builder.placeholderResId
    val errorResId: Int = builder.errorResId
    val isSkipMemoryCache: Boolean = builder.skipMemoryCache
    val isDiskCacheEnabled: Boolean = builder.diskCacheEnabled

    fun hasPlaceholder(): Boolean = placeholderResId != 0
    fun hasError(): Boolean = errorResId != 0

    class Builder {
        internal var placeholderResId: Int = 0
        internal var errorResId: Int = 0
        internal var skipMemoryCache: Boolean = false
        internal var diskCacheEnabled: Boolean = true

        fun placeholder(resId: Int): Builder = apply { placeholderResId = resId }
        fun error(resId: Int): Builder = apply { errorResId = resId }
        fun skipMemoryCache(skip: Boolean): Builder = apply { skipMemoryCache = skip }
        fun diskCache(enabled: Boolean): Builder = apply { diskCacheEnabled = enabled }

        /** 既不读内存缓存也不读磁盘缓存，用于验证码 / 头像刷新等场景。 */
        fun noCache(): Builder = apply {
            skipMemoryCache = true
            diskCacheEnabled = false
        }

        fun build(): ImageOptions = ImageOptions(this)
    }

    companion object {
        fun none(): ImageOptions = builder().build()

        fun placeholder(resId: Int): ImageOptions = builder()
            .placeholder(resId)
            .error(resId)
            .build()

        fun builder(): Builder = Builder()
    }
}
