package com.example.testdemo.lib.base.ui

import androidx.compose.ui.graphics.Color

/**
 * 标题栏配置：由页面状态推导后交给基类渲染。
 *
 * @param title 中间标题
 * @param showBack 是否显示左侧返回（首页 / Tab 页设为 false）
 * @param backgroundColor 背景色，null 用主题 surface
 * @param contentColor 前景色，null 用主题 onSurface
 */
data class TitleBarConfig(
    val title: String,
    val showBack: Boolean = true,
    val backgroundColor: Color? = null,
    val contentColor: Color? = null,
)
