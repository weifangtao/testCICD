package com.example.testdemo.lib.base.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** 标题栏高度（微信风格）。 */
private val TITLE_BAR_HEIGHT = 48.dp

/** 返回按钮可点区域宽度，同时也是右侧操作区的最小占位宽度。 */
private val TITLE_BAR_SIDE_WIDTH = 56.dp

/**
 * 统一标题栏：**左返回 / 中标题 / 右操作区**。
 *
 * 标题绝对居中，左右区域悬浮其上、宽度互不影响 —— 即微信那种样式。
 * 内部已处理状态栏内边距，配合 `enableEdgeToEdge()` 使用。
 *
 * ```
 * AppTitleBar(
 *     title = "详情",
 *     onBack = { finish() },
 *     actions = {
 *         // 文字
 *         TitleBarTextAction("保存") { save() }
 *         // 图片
 *         TitleBarIconAction(painterResource(R.drawable.ic_more), "更多") { more() }
 *     },
 * )
 * ```
 */
@Composable
fun AppTitleBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .statusBarsPadding()
                .height(TITLE_BAR_HEIGHT),
        ) {
            // 左：返回
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .clickable(onClick = onBack)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BackChevron(color = contentColor)
                }
            } else {
                // 无返回时留出对称占位，保证标题真正居中
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .padding(horizontal = TITLE_BAR_SIDE_WIDTH / 2),
                )
            }

            // 中：标题（绝对居中）
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = TITLE_BAR_SIDE_WIDTH + 16.dp),
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            // 右：操作区
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/** 右侧文字操作，如「保存」「更多」。 */
@Composable
fun TitleBarTextAction(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) LocalContentColor.current
            else LocalContentColor.current.copy(alpha = 0.4f),
            maxLines = 1,
        )
    }
}

/** 右侧图片操作，如「更多」「扫一扫」「头像」。 */
@Composable
fun TitleBarIconAction(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) LocalContentColor.current
            else LocalContentColor.current.copy(alpha = 0.4f),
        )
    }
}

/**
 * 右侧任意自定义内容（如头像、红点角标）。
 *
 * ```
 * TitleBarCustomAction { AppImage(url, Modifier.size(28.dp).clip(CircleShape)) }
 * ```
 */
@Composable
fun TitleBarCustomAction(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 用 Canvas 画返回箭头，避免额外引入 material-icons 依赖。 */
@Composable
private fun BackChevron(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.085f
        drawLine(
            color = color,
            start = Offset(w * 0.62f, h * 0.20f),
            end = Offset(w * 0.32f, h * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.32f, h * 0.50f),
            end = Offset(w * 0.62f, h * 0.80f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
