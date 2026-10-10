// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    // Hilt 与 KSP 必须在同一作用域声明，否则 Hilt 插件找不到 KSP 的 task class
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}